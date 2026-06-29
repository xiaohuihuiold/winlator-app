package com.xhhold.winlator.ui.screens.settings

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.winlator.box64.Box64Preset
import com.winlator.box64.Box64PresetManager
import com.winlator.core.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class SettingsOption(
    val value: String,
    val label: String,
)

data class SettingsUiState(
    val cursorSpeed: Float = 100f,
    val cursorScale: Float = 100f,
    val cursorColor: Int = 0xffffff,
    val moveCursorToTouchpoint: Boolean = false,
    val capturePointerOnExternalMouse: Boolean = true,
    val openAndroidBrowserFromWine: Boolean = true,
    val useAndroidClipboardOnWine: Boolean = false,
    val appTheme: Int = APP_THEME_DARK,
    val languageIndex: Int = 0,
    val enableWineDebug: Boolean = false,
    val box64Logs: Int = 0,
    val saveLogsToFile: Boolean = false,
    val logFile: String = defaultLogFile().path,
    val box64Preset: String = Box64Preset.DEFAULT,
    val box64Presets: List<SettingsOption> = emptyList(),
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val initialState = loadState()

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<SettingsUiState> = _state

    fun setCursorSpeed(value: Float) = mutate { it.copy(cursorSpeed = value) }
    fun setCursorScale(value: Float) = mutate { it.copy(cursorScale = value) }
    fun setCursorColor(value: Int) = mutate { it.copy(cursorColor = value) }
    fun setMoveCursorToTouchpoint(value: Boolean) = mutate { it.copy(moveCursorToTouchpoint = value) }
    fun setCapturePointerOnExternalMouse(value: Boolean) = mutate { it.copy(capturePointerOnExternalMouse = value) }
    fun setOpenAndroidBrowserFromWine(value: Boolean) = mutate { it.copy(openAndroidBrowserFromWine = value) }
    fun setUseAndroidClipboardOnWine(value: Boolean) = mutate { it.copy(useAndroidClipboardOnWine = value) }
    fun setAppTheme(value: Int) = mutate { it.copy(appTheme = value) }
    fun setLanguageIndex(value: Int) = mutate { it.copy(languageIndex = value) }
    fun setEnableWineDebug(value: Boolean) = mutate { it.copy(enableWineDebug = value) }
    fun setBox64Logs(value: Int) = mutate { it.copy(box64Logs = value) }
    fun setSaveLogsToFile(value: Boolean) = mutate { it.copy(saveLogsToFile = value) }
    fun setLogFile(value: String) = mutate { it.copy(logFile = value) }
    fun setBox64Preset(value: String) = mutate { it.copy(box64Preset = value) }

    fun save(): Boolean {
        val current = state.value
        preferences.edit()
            .putBoolean("move_cursor_to_touchpoint", current.moveCursorToTouchpoint)
            .putBoolean("capture_pointer_on_external_mouse", current.capturePointerOnExternalMouse)
            .putFloat("cursor_speed", current.cursorSpeed / 100f)
            .putFloat("cursor_scale", current.cursorScale / 100f)
            .putInt("cursor_color", current.cursorColor)
            .putBoolean("open_android_browser_from_wine", current.openAndroidBrowserFromWine)
            .putBoolean("use_android_clipboard_on_wine", current.useAndroidClipboardOnWine)
            .putInt("app_theme", current.appTheme)
            .putInt("lc_index", current.languageIndex)
            .putBoolean("enable_wine_debug", current.enableWineDebug)
            .putInt("box64_logs", current.box64Logs)
            .putBoolean("save_logs_to_file", current.saveLogsToFile)
            .putString("log_file", current.logFile.trim().ifEmpty { defaultLogFile().path })
            .putString("box64_preset", current.box64Preset)
            .apply()
        return current.appTheme != initialState.appTheme || current.languageIndex != initialState.languageIndex
    }

    private fun loadState(): SettingsUiState {
        val app = getApplication<Application>()
        val languageIndex = preferences.getInt("lc_index", LocaleHelper.getLocaleIndex(app))
        return SettingsUiState(
            cursorSpeed = preferences.getFloat("cursor_speed", 1.0f) * 100f,
            cursorScale = preferences.getFloat("cursor_scale", 1.0f) * 100f,
            cursorColor = preferences.getInt("cursor_color", 0xffffff),
            moveCursorToTouchpoint = preferences.getBoolean("move_cursor_to_touchpoint", false),
            capturePointerOnExternalMouse = preferences.getBoolean("capture_pointer_on_external_mouse", true),
            openAndroidBrowserFromWine = preferences.getBoolean("open_android_browser_from_wine", true),
            useAndroidClipboardOnWine = preferences.getBoolean("use_android_clipboard_on_wine", false),
            appTheme = preferences.getInt("app_theme", APP_THEME_DARK),
            languageIndex = languageIndex.coerceIn(0, 2),
            enableWineDebug = preferences.getBoolean("enable_wine_debug", false),
            box64Logs = preferences.getInt("box64_logs", 0),
            saveLogsToFile = preferences.getBoolean("save_logs_to_file", false),
            logFile = preferences.getString("log_file", defaultLogFile().path) ?: defaultLogFile().path,
            box64Preset = preferences.getString("box64_preset", Box64Preset.DEFAULT) ?: Box64Preset.DEFAULT,
            box64Presets = Box64PresetManager.getPresets(app).map { SettingsOption(it.id, it.name) },
        )
    }

    private fun mutate(block: (SettingsUiState) -> SettingsUiState) {
        _state.value = block(_state.value)
    }
}

const val APP_THEME_LIGHT = 0
const val APP_THEME_DARK = 1

fun defaultLogFile(): File {
    val parent = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Winlator")
    return File(parent, "logs.txt")
}
