package com.xhhold.winlator.ui.screens.shortcuts

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.winlator.box64.Box64PresetManager
import com.winlator.container.AudioDrivers
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.container.DXWrappers
import com.winlator.container.GraphicsDrivers
import com.winlator.container.Shortcut
import com.winlator.core.FileUtils
import com.winlator.core.StringUtils
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.winhandler.GamepadHandler
import com.xhhold.winlator.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.Executors

enum class ShortcutViewStyle {
    LIST,
    GRID,
}

data class ShortcutOption(
    val value: String,
    val label: String,
)

data class ShortcutContainerOption(
    val id: Int,
    val name: String,
)

data class ShortcutSettingsUiState(
    val shortcut: Shortcut,
    val name: String,
    val execArgs: String,
    val screenSize: String,
    val customWidth: String,
    val customHeight: String,
    val vulkanDriver: String,
    val openglDriver: String,
    val graphicsDriverConfig: String,
    val dxWrapper: String,
    val dxWrapperConfig: String,
    val audioDriver: String,
    val audioDriverConfig: String,
    val forceFullscreen: Boolean,
    val box64Preset: String,
    val controlsProfile: String,
    val dinputMapperType: Int,
    val envVars: String,
    val winComponents: String,
    val errorMessage: String? = null,
)

data class ShortcutsUiState(
    val title: String = "",
    val shortcuts: List<Shortcut> = emptyList(),
    val viewStyle: ShortcutViewStyle = ShortcutViewStyle.GRID,
    val canGoBack: Boolean = false,
    val canPaste: Boolean = false,
    val isBusy: Boolean = false,
    val containers: List<ShortcutContainerOption> = emptyList(),
    val currentFolderContainerId: Int? = null,
    val message: String? = null,
    val settings: ShortcutSettingsUiState? = null,
    val box64Presets: List<ShortcutOption> = emptyList(),
    val controlsProfiles: List<ShortcutOption> = emptyList(),
)

class ShortcutsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val manager = ContainerManager(context)
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val folderStack = mutableListOf<Shortcut>()
    private var clipboard: ShortcutClipboard? = null

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<ShortcutsUiState> = _state

    fun refresh() {
        refreshContent()
    }

    fun setViewStyle(viewStyle: ShortcutViewStyle) {
        preferences.edit().putString("shortcuts_view_style", viewStyle.name).apply()
        mutate { it.copy(viewStyle = viewStyle) }
    }

    fun openFolder(shortcut: Shortcut) {
        if (!shortcut.file.isDirectory) return
        folderStack.add(shortcut)
        clearClipboard(emit = false)
        refreshContent()
    }

    fun goBack(): Boolean {
        clearClipboard(emit = false)
        if (folderStack.isEmpty()) return false
        folderStack.removeAt(folderStack.lastIndex)
        refreshContent()
        return true
    }

    fun createFolder(containerId: Int?, name: String) {
        val cleanName = StringUtils.clearReservedChars(name.trim())
        if (cleanName.isEmpty()) return

        val parent = folderStack.lastOrNull()?.file
            ?: containerId?.let { manager.getContainerById(it) }?.let { File(it.userDir, "Desktop") }
            ?: return

        if (!parent.isDirectory) parent.mkdirs()
        val folder = File(parent, cleanName)
        if (folder.exists()) {
            showMessage(context.getString(R.string.there_already_file_with_that_name))
            return
        }
        if (folder.mkdir()) {
            clearClipboard(emit = false)
            refreshContent()
        }
    }

    fun copyShortcut(shortcut: Shortcut, cutMode: Boolean) {
        val files = mutableListOf(File(shortcut.file.parentFile, shortcut.file.name))
        if (shortcut.file.isFile) {
            files.add(shortcut.getLinkFile())
        }
        clipboard = ShortcutClipboard(files, cutMode)
        mutate { it.copy(canPaste = true) }
    }

    fun pasteFiles() {
        val currentClipboard = clipboard ?: return
        val targetDir = folderStack.lastOrNull()?.file
        if (targetDir == null) {
            clearClipboard(emit = false)
            showMessage(context.getString(R.string.you_cannot_paste_files_here))
            return
        }

        for (file in currentClipboard.files) {
            val targetFile = File(targetDir, file.name)
            if (targetFile.exists()) {
                showMessage(context.getString(R.string.there_already_file_with_that_name))
                return
            }
        }

        mutate { it.copy(isBusy = true) }
        executor.execute {
            for (originFile in currentClipboard.files) {
                if (!originFile.exists()) continue
                val targetFile = File(targetDir, originFile.name)
                if (FileUtils.copy(originFile, targetFile) && currentClipboard.cutMode) {
                    FileUtils.delete(originFile)
                }
            }
            handler.post {
                clipboard = null
                refreshContent(isBusy = false)
            }
        }
    }

    fun removeShortcut(shortcut: Shortcut) {
        mutate { it.copy(isBusy = true) }
        executor.execute {
            shortcut.remove()
            handler.post {
                clearClipboard(emit = false)
                refreshContent(isBusy = false)
            }
        }
    }

    fun editShortcut(shortcut: Shortcut) {
        if (shortcut.file.isDirectory) return
        mutate { it.copy(settings = shortcut.toSettings()) }
    }

    fun updateSettings(block: (ShortcutSettingsUiState) -> ShortcutSettingsUiState) {
        mutate { current ->
            val settings = current.settings ?: return@mutate current
            current.copy(settings = block(settings))
        }
    }

    fun dismissSettings() {
        mutate { it.copy(settings = null) }
    }

    fun saveSettings() {
        val settings = state.value.settings ?: return
        val safeName = StringUtils.clearReservedChars(settings.name.trim())
        if (safeName.isEmpty()) {
            mutate { it.copy(settings = settings.copy(errorMessage = context.getString(R.string.name_is_required))) }
            return
        }

        val shortcut = settings.shortcut
        val targetFile = File(shortcut.file.parentFile, "$safeName.desktop")
        if (safeName != shortcut.name && targetFile.exists()) {
            mutate {
                it.copy(settings = settings.copy(errorMessage = context.getString(R.string.there_already_file_with_that_name)))
            }
            return
        }

        saveShortcutData(settings)
        if (safeName != shortcut.name) {
            renameShortcut(shortcut, safeName)
        }
        clearClipboard(emit = false)
        refreshContent(settings = null)
    }

    fun clearMessage() {
        mutate { it.copy(message = null) }
    }

    private fun createInitialState(): ShortcutsUiState {
        val viewStyle = runCatching {
            ShortcutViewStyle.valueOf(preferences.getString("shortcuts_view_style", ShortcutViewStyle.GRID.name) ?: ShortcutViewStyle.GRID.name)
        }.getOrDefault(ShortcutViewStyle.GRID)

        return ShortcutsUiState(
            title = context.getString(R.string.shortcuts),
            viewStyle = viewStyle,
            shortcuts = manager.loadShortcuts(null),
            containers = containerOptions(),
            box64Presets = Box64PresetManager.getPresets(context).map { ShortcutOption(it.id, it.name) },
            controlsProfiles = controlsProfileOptions(),
        )
    }

    private fun refreshContent(
        isBusy: Boolean = state.value.isBusy,
        settings: ShortcutSettingsUiState? = state.value.settings,
    ) {
        val selectedFolder = folderStack.lastOrNull()
        mutate {
            it.copy(
                title = selectedFolder?.name ?: context.getString(R.string.shortcuts),
                shortcuts = manager.loadShortcuts(selectedFolder),
                canGoBack = selectedFolder != null,
                canPaste = clipboard != null,
                isBusy = isBusy,
                containers = containerOptions(),
                currentFolderContainerId = selectedFolder?.container?.id,
                settings = settings,
                box64Presets = Box64PresetManager.getPresets(context).map { preset -> ShortcutOption(preset.id, preset.name) },
                controlsProfiles = controlsProfileOptions(),
            )
        }
    }

    private fun saveShortcutData(settings: ShortcutSettingsUiState) {
        val shortcut = settings.shortcut
        val container = shortcut.container
        val screenSize = resolveScreenSize(settings)
        val graphicsDriver = "${settings.vulkanDriver},${settings.openglDriver}"

        shortcut.putExtra("execArgs", settings.execArgs.ifBlank { null })
        shortcut.putExtra("screenSize", screenSize.takeIf { it != container.screenSize })
        shortcut.putExtra("graphicsDriver", graphicsDriver.takeIf { it != container.graphicsDriver })
        shortcut.putExtra("graphicsDriverConfig", settings.graphicsDriverConfig.takeIf { it != container.graphicsDriverConfig })
        shortcut.putExtra("dxwrapper", settings.dxWrapper.takeIf { it != container.dxWrapper })
        shortcut.putExtra("dxwrapperConfig", settings.dxWrapperConfig.takeIf { it != container.dxWrapperConfig })
        shortcut.putExtra("audioDriver", settings.audioDriver.takeIf { it != container.audioDriver })
        shortcut.putExtra("audioDriverConfig", settings.audioDriverConfig.takeIf { it != container.audioDriverConfig })
        shortcut.putExtra("forceFullscreen", if (settings.forceFullscreen) "1" else null)
        shortcut.putExtra("box64Preset", settings.box64Preset.takeIf { it != container.box64Preset })
        shortcut.putExtra("controlsProfile", settings.controlsProfile.takeIf { it != "0" })
        shortcut.putExtra(
            "dinputMapperType",
            settings.dinputMapperType.takeIf { it != GamepadHandler.DINPUT_MAPPER_TYPE_XINPUT.toInt() }?.toString(),
        )
        shortcut.putExtra("envVars", settings.envVars.ifBlank { null })
        shortcut.putExtra("wincomponents", settings.winComponents.takeIf { it != container.winComponents })
        shortcut.saveData()
    }

    private fun renameShortcut(shortcut: Shortcut, newName: String) {
        val parent = shortcut.file.parentFile ?: return
        val newDesktopFile = File(parent, "$newName.desktop")
        if (!newDesktopFile.exists()) {
            shortcut.file.renameTo(newDesktopFile)
        }

        val linkFile = File(parent, "${shortcut.name}.lnk")
        if (linkFile.isFile) {
            val newLinkFile = File(parent, "$newName.lnk")
            if (!newLinkFile.exists()) {
                linkFile.renameTo(newLinkFile)
            }
        }
    }

    private fun Shortcut.toSettings(): ShortcutSettingsUiState {
        val graphics = GraphicsDrivers.parseIdentifiers(getExtra("graphicsDriver", container.graphicsDriver))
        val parsedScreen = parseScreenSize(getExtra("screenSize", container.screenSize))
        return ShortcutSettingsUiState(
            shortcut = this,
            name = name,
            execArgs = getExtra("execArgs"),
            screenSize = parsedScreen.first,
            customWidth = parsedScreen.second,
            customHeight = parsedScreen.third,
            vulkanDriver = graphics.getOrElse(0) { GraphicsDrivers.DEFAULT_VULKAN_DRIVER },
            openglDriver = graphics.getOrElse(1) { GraphicsDrivers.DEFAULT_OPENGL_DRIVER },
            graphicsDriverConfig = getExtra("graphicsDriverConfig", container.graphicsDriverConfig),
            dxWrapper = DXWrappers.parseIdentifier(getExtra("dxwrapper", container.dxWrapper)),
            dxWrapperConfig = getExtra("dxwrapperConfig", container.dxWrapperConfig),
            audioDriver = getExtra("audioDriver", container.audioDriver).ifBlank { AudioDrivers.ALSA },
            audioDriverConfig = getExtra("audioDriverConfig", container.audioDriverConfig),
            forceFullscreen = getExtra("forceFullscreen", "0") == "1",
            box64Preset = getExtra("box64Preset", container.box64Preset),
            controlsProfile = getExtra("controlsProfile", "0"),
            dinputMapperType = getExtra("dinputMapperType", GamepadHandler.DINPUT_MAPPER_TYPE_XINPUT.toString()).toIntOrNull()
                ?: GamepadHandler.DINPUT_MAPPER_TYPE_XINPUT.toInt(),
            envVars = getExtra("envVars"),
            winComponents = getExtra("wincomponents", container.winComponents),
        )
    }

    private fun resolveScreenSize(settings: ShortcutSettingsUiState): String {
        if (settings.screenSize != CUSTOM_SCREEN_SIZE) return settings.screenSize
        val width = settings.customWidth.toIntOrNull()
        val height = settings.customHeight.toIntOrNull()
        return if (width != null && height != null && width > 0 && height > 0 && width % 2 == 0 && height % 2 == 0) {
            "${width}x${height}"
        }
        else {
            Container.DEFAULT_SCREEN_SIZE
        }
    }

    private fun parseScreenSize(value: String): Triple<String, String, String> {
        val parts = value.split("x")
        val width = parts.getOrNull(0)?.filter(Char::isDigit).orEmpty().ifBlank { "1280" }
        val height = parts.getOrNull(1)?.filter(Char::isDigit).orEmpty().ifBlank { "720" }
        val selection = if (STANDARD_SCREEN_SIZES.contains(value)) value else CUSTOM_SCREEN_SIZE
        return Triple(selection, width, height)
    }

    private fun containerOptions(): List<ShortcutContainerOption> {
        return manager.containers.map { ShortcutContainerOption(it.id, it.name) }
    }

    private fun controlsProfileOptions(): List<ShortcutOption> {
        val controlsManager = InputControlsManager(context)
        return listOf(ShortcutOption("0", context.getString(R.string.none))) +
            controlsManager.getProfiles(true).map { ShortcutOption(it.id.toString(), it.name) }
    }

    private fun clearClipboard(emit: Boolean) {
        clipboard = null
        if (emit) mutate { it.copy(canPaste = false) }
    }

    private fun showMessage(message: String) {
        mutate { it.copy(message = message, isBusy = false, canPaste = clipboard != null) }
    }

    private fun mutate(block: (ShortcutsUiState) -> ShortcutsUiState) {
        _state.value = block(_state.value)
    }

    private data class ShortcutClipboard(
        val files: List<File>,
        val cutMode: Boolean,
    )

    companion object {
        const val CUSTOM_SCREEN_SIZE = "custom"

        val STANDARD_SCREEN_SIZES = setOf(
            "640x360",
            "640x480",
            "800x600",
            "854x480",
            "960x544",
            "1024x768",
            "1280x720",
            "1280x800",
            "1280x1024",
            "1366x768",
            "1440x900",
            "1600x900",
            "1920x1080",
        )
    }
}
