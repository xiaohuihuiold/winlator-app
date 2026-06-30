package com.xhhold.winlator.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.winlator.box64.Box64Preset
import com.winlator.box64.Box64PresetManager
import com.winlator.core.AppUtils
import com.winlator.container.AudioDrivers
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.container.DXWrappers
import com.winlator.container.GraphicsDrivers
import com.winlator.core.EnvVars
import com.winlator.core.KeyValueSet
import com.winlator.core.WineInfo
import com.winlator.core.WineInstaller
import com.winlator.widget.FrameRating
import com.winlator.widget.EnvVarsView
import com.xhhold.winlator.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import org.json.JSONObject

data class DetailOption(
    val value: String,
    val label: String,
)

enum class DetailEnvVarType {
    CHECKBOX,
    SELECT,
    SELECT_MULTIPLE,
    TEXT,
    NUMBER,
}

data class DetailKnownEnvVar(
    val name: String,
    val type: DetailEnvVarType,
    val options: List<String> = emptyList(),
)

data class DetailEnvVarItem(
    val name: String,
    val value: String,
)

data class DetailDriveItem(
    val letter: String,
    val path: String,
)

data class DetailWinComponentItem(
    val name: String,
    val value: Int,
)

data class DetailUiState(
    val exists: Boolean = true,
    val isEditMode: Boolean = false,
    val titleName: String = "",
    val name: String = "",
    val screenSize: String = Container.DEFAULT_SCREEN_SIZE,
    val customWidth: String = "1280",
    val customHeight: String = "720",
    val wineVersion: String = WineInfo.MAIN_WINE_INFO.identifier(),
    val wineVersions: List<DetailOption> = emptyList(),
    val vulkanDriver: String = GraphicsDrivers.DEFAULT_VULKAN_DRIVER,
    val openglDriver: String = GraphicsDrivers.DEFAULT_OPENGL_DRIVER,
    val dxWrapper: String = Container.DEFAULT_DXWRAPPER,
    val dxWrapperConfig: String = "",
    val graphicsDriverConfig: String = "",
    val audioDriver: String = Container.DEFAULT_AUDIO_DRIVER,
    val audioDriverConfig: String = "",
    val hudMode: Int = FrameRating.Mode.DISABLED.ordinal,
    val startupSelection: Int = Container.STARTUP_SELECTION_ESSENTIAL.toInt(),
    val box64Preset: String = Box64Preset.DEFAULT,
    val box64Presets: List<DetailOption> = emptyList(),
    val envVars: String = Container.DEFAULT_ENV_VARS,
    val envVarItems: List<DetailEnvVarItem> = emptyList(),
    val winComponents: String = Container.DEFAULT_WINCOMPONENTS,
    val winComponentItems: List<DetailWinComponentItem> = emptyList(),
    val drives: String = Container.DEFAULT_DRIVES,
    val driveItems: List<DetailDriveItem> = emptyList(),
    val driveLocations: List<DetailOption> = emptyList(),
    val cpuList: String? = Container.getFallbackCPUList(),
    val cpuListWoW64: String? = Container.getFallbackCPUList(),
    val desktopTheme: String = com.winlator.core.WineThemeManager.DEFAULT_DESKTOP_THEME,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class DetailViewModel(
    application: Application,
    private val containerId: Int?,
) : AndroidViewModel(application) {
    private val context = application
    private val manager = ContainerManager(context)
    private val container = containerId?.takeIf { it > 0 }?.let { manager.getContainerById(it) }

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<DetailUiState> = _state

    fun setName(value: String) = mutate { it.copy(name = value, errorMessage = null) }
    fun setScreenSize(value: String) = mutate { it.copy(screenSize = value) }
    fun setCustomWidth(value: String) = mutate { it.copy(customWidth = value.filter(Char::isDigit)) }
    fun setCustomHeight(value: String) = mutate { it.copy(customHeight = value.filter(Char::isDigit)) }
    fun setWineVersion(value: String) = mutate { it.copy(wineVersion = value) }
    fun setVulkanDriver(value: String) = mutate { it.copy(vulkanDriver = value) }
    fun setOpenGLDriver(value: String) = mutate { it.copy(openglDriver = value) }
    fun setDXWrapper(value: String) = mutate { it.copy(dxWrapper = value) }
    fun setAudioDriver(value: String) = mutate { it.copy(audioDriver = value) }
    fun setHudMode(value: Int) = mutate { it.copy(hudMode = value) }
    fun setStartupSelection(value: Int) = mutate { it.copy(startupSelection = value) }
    fun setBox64Preset(value: String) = mutate { it.copy(box64Preset = value) }
    fun setEnvVars(value: String) = mutate { it.copy(envVars = value, envVarItems = parseEnvVars(value)) }
    fun setWinComponents(value: String) = mutate {
        it.copy(winComponents = value, winComponentItems = parseWinComponents(value))
    }
    fun setDrives(value: String) = mutate { it.copy(drives = value, driveItems = parseDrives(value)) }

    fun addEnvVar(name: String, initialValue: String? = null) = mutate { current ->
        val cleanName = name.trim().replace(" ", "")
        if (cleanName.isEmpty() || current.envVarItems.any { it.name == cleanName }) {
            current
        }
        else {
            val cleanValue = initialValue?.trim()?.replace(" ", "")
                ?: defaultEnvVarValue(cleanName)
            current.withEnvVarItems(current.envVarItems + DetailEnvVarItem(cleanName, cleanValue))
        }
    }

    fun updateEnvVarName(index: Int, value: String) = mutate { current ->
        current.withEnvVarItems(
            current.envVarItems.mapIndexed { itemIndex, item ->
                if (itemIndex == index) item.copy(name = value.trim().replace(" ", "")) else item
            },
        )
    }

    fun updateEnvVarValue(index: Int, value: String) = mutate { current ->
        current.withEnvVarItems(
            current.envVarItems.mapIndexed { itemIndex, item ->
                if (itemIndex == index) item.copy(value = value.trim().replace(" ", "")) else item
            },
        )
    }

    fun removeEnvVar(index: Int) = mutate { current ->
        current.withEnvVarItems(current.envVarItems.filterIndexed { itemIndex, _ -> itemIndex != index })
    }

    fun addDrive() = mutate { current ->
        val usedLetters = current.driveItems.map { it.letter }.toSet()
        val nextLetter = DRIVE_LETTERS.firstOrNull { it !in usedLetters } ?: return@mutate current
        current.withDriveItems(current.driveItems + DetailDriveItem(nextLetter, ""))
    }

    fun updateDriveLetter(index: Int, value: String) = mutate { current ->
        current.withDriveItems(
            current.driveItems.mapIndexed { itemIndex, item ->
                if (itemIndex == index) item.copy(letter = value) else item
            },
        )
    }

    fun updateDrivePath(index: Int, value: String) = mutate { current ->
        current.withDriveItems(
            current.driveItems.mapIndexed { itemIndex, item ->
                if (itemIndex == index) item.copy(path = value) else item
            },
        )
    }

    fun removeDrive(index: Int) = mutate { current ->
        current.withDriveItems(current.driveItems.filterIndexed { itemIndex, _ -> itemIndex != index })
    }

    fun updateWinComponent(index: Int, value: Int) = mutate { current ->
        current.withWinComponentItems(
            current.winComponentItems.mapIndexed { itemIndex, item ->
                if (itemIndex == index) item.copy(value = value) else item
            },
        )
    }

    fun save(onSaved: () -> Unit) {
        val current = state.value
        if (current.isSaving) return
        val name = current.name.trim()
        if (name.isEmpty()) {
            mutate { it.copy(errorMessage = context.getString(R.string.name_is_required)) }
            return
        }

        if (current.isEditMode) {
            val target = container ?: return mutate { it.copy(exists = false) }
            applyToContainer(target, current.copy(name = name))
            target.saveData()
            onSaved()
            return
        }

        val data = createContainerData(current.copy(name = name)) ?: return
        mutate { it.copy(isSaving = true, errorMessage = null) }
        manager.createContainerAsync(data) { createdContainer ->
            if (createdContainer != null) {
                onSaved()
            }
            else {
                mutate { it.copy(isSaving = false, errorMessage = context.getString(R.string.unable_to_create_container)) }
            }
        }
    }

    private fun createInitialState(): DetailUiState {
        val wineVersions = WineInstaller.getInstalledWineInfos(context).map {
            DetailOption(it.identifier(), it.toString())
        }.ifEmpty {
            listOf(DetailOption(WineInfo.MAIN_WINE_INFO.identifier(), WineInfo.MAIN_WINE_INFO.toString()))
        }
        val box64Presets = Box64PresetManager.getPresets(context).map { DetailOption(it.id, it.name) }
        val target = container

        if (containerId != null && containerId > 0 && target == null) {
            return DetailUiState(
                exists = false,
                wineVersions = wineVersions,
                box64Presets = box64Presets,
                driveLocations = driveLocationOptions(),
            )
        }

        if (target == null) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val nextName = "Container-${manager.nextContainerId}"
            val graphics = GraphicsDrivers.parseIdentifiers(GraphicsDrivers.getDefaultDriver(context))
            return DetailUiState(
                isEditMode = false,
                titleName = nextName,
                name = nextName,
                wineVersions = wineVersions,
                vulkanDriver = graphics[0],
                openglDriver = graphics[1],
                box64Preset = preferences.getString("box64_preset", Box64Preset.DEFAULT) ?: Box64Preset.DEFAULT,
                box64Presets = box64Presets,
                envVarItems = parseEnvVars(Container.DEFAULT_ENV_VARS),
                winComponentItems = parseWinComponents(Container.DEFAULT_WINCOMPONENTS),
                driveItems = parseDrives(Container.DEFAULT_DRIVES),
                driveLocations = driveLocationOptions(),
            )
        }

        val graphics = GraphicsDrivers.parseIdentifiers(target.graphicsDriver)
        val parsedScreen = parseScreenSize(target.screenSize)
        return DetailUiState(
            isEditMode = true,
            titleName = target.name,
            name = target.name,
            screenSize = parsedScreen.first,
            customWidth = parsedScreen.second,
            customHeight = parsedScreen.third,
            wineVersion = target.wineVersion,
            wineVersions = wineVersions,
            vulkanDriver = graphics[0],
            openglDriver = graphics[1],
            dxWrapper = DXWrappers.parseIdentifier(target.dxWrapper),
            dxWrapperConfig = target.dxWrapperConfig,
            graphicsDriverConfig = target.graphicsDriverConfig,
            audioDriver = target.audioDriver,
            audioDriverConfig = target.audioDriverConfig,
            hudMode = target.hudMode.toInt(),
            startupSelection = target.startupSelection.toInt(),
            box64Preset = target.box64Preset,
            box64Presets = box64Presets,
            envVars = target.envVars,
            envVarItems = parseEnvVars(target.envVars),
            winComponents = target.winComponents,
            winComponentItems = parseWinComponents(target.winComponents),
            drives = target.drives,
            driveItems = parseDrives(target.drives),
            driveLocations = driveLocationOptions(),
            cpuList = target.getCPUList(true),
            cpuListWoW64 = target.getCPUListWoW64(true),
            desktopTheme = target.desktopTheme,
        )
    }

    private fun applyToContainer(target: Container, current: DetailUiState) {
        target.name = current.name
        target.screenSize = resolveScreenSize(current)
        target.envVars = current.envVars
        target.cpuList = current.cpuList
        target.cpuListWoW64 = current.cpuListWoW64
        target.graphicsDriver = "${current.vulkanDriver},${current.openglDriver}"
        target.graphicsDriverConfig = current.graphicsDriverConfig
        target.dxWrapper = current.dxWrapper
        target.dxWrapperConfig = current.dxWrapperConfig
        target.audioDriver = current.audioDriver
        target.audioDriverConfig = current.audioDriverConfig
        target.winComponents = current.winComponents
        target.drives = current.drives
        target.hudMode = current.hudMode.toByte()
        target.startupSelection = current.startupSelection.toByte()
        target.box64Preset = current.box64Preset
        target.desktopTheme = current.desktopTheme
    }

    private fun createContainerData(current: DetailUiState): JSONObject? {
        return try {
            JSONObject().apply {
                put("name", current.name)
                put("screenSize", resolveScreenSize(current))
                put("envVars", current.envVars)
                put("cpuList", current.cpuList)
                put("cpuListWoW64", current.cpuListWoW64)
                put("graphicsDriver", "${current.vulkanDriver},${current.openglDriver}")
                put("dxwrapper", current.dxWrapper)
                put("dxwrapperConfig", current.dxWrapperConfig)
                put("graphicsDriverConfig", current.graphicsDriverConfig)
                put("audioDriver", current.audioDriver)
                put("audioDriverConfig", current.audioDriverConfig)
                put("wincomponents", current.winComponents)
                put("drives", current.drives)
                put("hudMode", current.hudMode.toByte())
                put("startupSelection", current.startupSelection.toByte())
                put("box64Preset", current.box64Preset)
                put("desktopTheme", current.desktopTheme)
                if (!WineInfo.isMainWineVersion(current.wineVersion)) {
                    put("wineVersion", current.wineVersion)
                }
            }
        }
        catch (e: JSONException) {
            mutate { it.copy(errorMessage = e.message ?: context.getString(R.string.unable_to_create_container)) }
            null
        }
    }

    private fun resolveScreenSize(current: DetailUiState): String {
        if (current.screenSize != CUSTOM_SCREEN_SIZE) return current.screenSize
        val width = current.customWidth.toIntOrNull()
        val height = current.customHeight.toIntOrNull()
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

    private fun parseEnvVars(value: String): List<DetailEnvVarItem> {
        if (value.isBlank()) return emptyList()
        return value.split(" ")
            .mapNotNull { item ->
                val index = item.indexOf("=")
                if (index <= 0) {
                    null
                }
                else {
                    DetailEnvVarItem(
                        name = item.substring(0, index),
                        value = item.substring(index + 1),
                    )
                }
            }
    }

    private fun serializeEnvVars(items: List<DetailEnvVarItem>): String {
        val envVars = EnvVars()
        items.forEach { item ->
            val name = item.name.trim().replace(" ", "")
            val value = item.value.trim().replace(" ", "")
            if (name.isNotEmpty() && value.isNotEmpty()) {
                envVars.put(name, value)
            }
        }
        return envVars.toString()
    }

    private fun parseDrives(value: String): List<DetailDriveItem> {
        return Container.drivesIterator(value).map { DetailDriveItem(it.letter, it.path) }
    }

    private fun serializeDrives(items: List<DetailDriveItem>): String {
        return items.joinToString(separator = "") { item ->
            val path = item.path.replace(":", "").trim()
            if (path.isNotEmpty()) "${item.letter}:$path" else ""
        }
    }

    private fun parseWinComponents(value: String): List<DetailWinComponentItem> {
        val parsedValues = mutableMapOf<String, Int>()
        for (item in KeyValueSet(value)) {
            parsedValues[item[0]] = item[1].toIntOrNull() ?: 0
        }
        val result = mutableListOf<DetailWinComponentItem>()
        for (item in KeyValueSet(Container.DEFAULT_WINCOMPONENTS)) {
            result.add(DetailWinComponentItem(item[0], parsedValues[item[0]] ?: item[1].toIntOrNull() ?: 0))
        }
        parsedValues.forEach { (name, value) ->
            if (result.none { it.name == name }) {
                result.add(DetailWinComponentItem(name, value))
            }
        }
        return result
    }

    private fun serializeWinComponents(items: List<DetailWinComponentItem>): String {
        return items.joinToString(",") { "${it.name}=${it.value.coerceIn(0, 1)}" }
    }

    private fun defaultEnvVarValue(name: String): String {
        val known = knownEnvVar(name) ?: return ""
        return when (known.type) {
            DetailEnvVarType.CHECKBOX,
            DetailEnvVarType.SELECT -> known.options.firstOrNull().orEmpty()

            DetailEnvVarType.SELECT_MULTIPLE,
            DetailEnvVarType.TEXT,
            DetailEnvVarType.NUMBER -> ""
        }
    }

    private fun driveLocationOptions(): List<DetailOption> {
        return buildList {
            add(DetailOption(AppUtils.DIRECTORY_DOWNLOADS, context.getString(R.string.downloads)))
            add(DetailOption(AppUtils.INTERNAL_STORAGE, context.getString(R.string.internal_storage)))
            manager.containers.forEach { container ->
                add(DetailOption("${container.rootDir}/.wine/drive_c", "${container.name} (Drive C:)"))
            }
        }
    }

    private fun DetailUiState.withEnvVarItems(items: List<DetailEnvVarItem>): DetailUiState {
        return copy(envVarItems = items, envVars = serializeEnvVars(items))
    }

    private fun DetailUiState.withDriveItems(items: List<DetailDriveItem>): DetailUiState {
        return copy(driveItems = items, drives = serializeDrives(items))
    }

    private fun DetailUiState.withWinComponentItems(items: List<DetailWinComponentItem>): DetailUiState {
        return copy(winComponentItems = items, winComponents = serializeWinComponents(items))
    }

    private fun mutate(block: (DetailUiState) -> DetailUiState) {
        _state.value = block(_state.value)
    }

    companion object {
        const val CUSTOM_SCREEN_SIZE = "custom"

        val DRIVE_LETTERS = (0 until Container.MAX_DRIVE_LETTERS.toInt()).map { ((it + 68).toChar()).toString() }

        val KNOWN_ENV_VARS: List<DetailKnownEnvVar> = EnvVarsView.knownEnvVars.map { values ->
            DetailKnownEnvVar(
                name = values[0],
                type = DetailEnvVarType.valueOf(values[1]),
                options = values.drop(2),
            )
        }

        fun knownEnvVar(name: String): DetailKnownEnvVar? {
            return KNOWN_ENV_VARS.firstOrNull { it.name == name }
        }

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
