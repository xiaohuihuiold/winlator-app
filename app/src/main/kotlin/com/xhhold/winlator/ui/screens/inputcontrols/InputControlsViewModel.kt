package com.xhhold.winlator.ui.screens.inputcontrols

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.winlator.core.FileUtils
import com.winlator.core.HttpUtils
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.ExternalController
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.widget.InputControlsView
import com.xhhold.winlator.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

data class InputProfileOption(
    val id: Int,
    val name: String,
)

data class ExternalControllerUiState(
    val id: String,
    val name: String,
    val bindingCount: Int,
    val connected: Boolean,
)

data class InputControlsUiState(
    val profiles: List<InputProfileOption> = emptyList(),
    val selectedProfileId: Int? = null,
    val cursorSpeed: Float = 100f,
    val overlayOpacity: Float = InputControlsView.DEFAULT_OVERLAY_OPACITY * 100f,
    val disableMouseInput: Boolean = false,
    val controllers: List<ExternalControllerUiState> = emptyList(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val downloadItems: List<String>? = null,
)

class InputControlsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val manager = InputControlsManager(context)
    private val handler = Handler(Looper.getMainLooper())
    private var currentProfile: ControlsProfile? = null

    private val _state = MutableStateFlow(createState())
    val state: StateFlow<InputControlsUiState> = _state

    fun refresh() {
        refreshState()
    }

    fun selectProfile(profileId: Int?) {
        currentProfile = profileId?.takeIf { it > 0 }?.let { manager.getProfile(it) }
        refreshState()
    }

    fun setCursorSpeed(value: Float) {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        profile.cursorSpeed = value / 100f
        profile.save()
        mutate { it.copy(cursorSpeed = value) }
    }

    fun setOverlayOpacity(value: Float) {
        preferences.edit().putFloat("overlay_opacity", value / 100f).apply()
        mutate { it.copy(overlayOpacity = value) }
    }

    fun setDisableMouseInput(value: Boolean) {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        profile.isDisableMouseInput = value
        profile.save()
        mutate { it.copy(disableMouseInput = value) }
    }

    fun createProfile(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        currentProfile = manager.createProfile(cleanName)
        refreshState()
    }

    fun renameSelectedProfile(name: String) {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        profile.name = cleanName
        profile.save()
        refreshState()
    }

    fun duplicateSelectedProfile() {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        currentProfile = manager.duplicateProfile(profile)
        refreshState()
    }

    fun removeSelectedProfile() {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        manager.removeProfile(profile)
        currentProfile = null
        refreshState()
    }

    fun importProfile(uri: Uri) {
        try {
            val imported = manager.importProfile(JSONObject(FileUtils.readString(context, uri)))
            if (imported != null) {
                currentProfile = imported
                refreshState()
            }
            else {
                showMessage(context.getString(R.string.unable_to_import_profile))
            }
        }
        catch (e: Exception) {
            showMessage(context.getString(R.string.unable_to_import_profile))
        }
    }

    fun exportSelectedProfile() {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        val exportedFile = manager.exportProfile(profile)
        if (exportedFile != null) {
            val path = exportedFile.path.substringAfter(Environment.DIRECTORY_DOWNLOADS, exportedFile.path)
            showMessage("${context.getString(R.string.profile_exported_to)} ${Environment.DIRECTORY_DOWNLOADS}$path")
        }
    }

    fun downloadProfileList() {
        mutate { it.copy(isBusy = true, downloadItems = null) }
        HttpUtils.download(String.format(INPUT_CONTROLS_URL, "index.txt")) { content ->
            handler.post {
                if (content != null) {
                    val items = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    if (items.isEmpty()) {
                        showMessage(context.getString(R.string.there_are_no_items_to_download))
                    }
                    else {
                        mutate { it.copy(isBusy = false, downloadItems = items) }
                    }
                }
                else {
                    showMessage(context.getString(R.string.a_network_error_occurred))
                }
            }
        }
    }

    fun downloadProfiles(items: List<String>) {
        if (items.isEmpty()) {
            dismissDownloadDialog()
            return
        }
        mutate { it.copy(isBusy = true, downloadItems = null) }
        currentProfile = null
        val processedItemCount = AtomicInteger()
        for (item in items) {
            HttpUtils.download(String.format(INPUT_CONTROLS_URL, item)) { content ->
                try {
                    if (content != null) {
                        manager.importProfile(JSONObject(content))
                    }
                }
                catch (e: JSONException) {
                }
                if (processedItemCount.incrementAndGet() == items.size) {
                    handler.post {
                        manager.loadProfiles(false)
                        refreshState(isBusy = false)
                    }
                }
            }
        }
    }

    fun dismissDownloadDialog() {
        mutate { it.copy(downloadItems = null) }
    }

    fun removeController(controllerId: String) {
        val profile = currentProfile ?: return showMessage(context.getString(R.string.no_profile_selected))
        val controller = profile.getController(controllerId) ?: return
        profile.removeController(controller)
        profile.save()
        refreshState()
    }

    fun requireProfile() {
        showMessage(context.getString(R.string.no_profile_selected))
    }

    fun clearMessage() {
        mutate { it.copy(message = null) }
    }

    private fun createState(): InputControlsUiState {
        return buildState()
    }

    private fun refreshState(isBusy: Boolean = state.value.isBusy) {
        mutate { buildState(isBusy = isBusy, message = it.message, downloadItems = it.downloadItems) }
    }

    private fun buildState(
        isBusy: Boolean = false,
        message: String? = null,
        downloadItems: List<String>? = null,
    ): InputControlsUiState {
        val profiles = manager.getProfiles()
        val selectedProfile = currentProfile?.let { selected ->
            profiles.firstOrNull { it.id == selected.id }
        }
        currentProfile = selectedProfile
        return InputControlsUiState(
            profiles = profiles.map { InputProfileOption(it.id, it.name) },
            selectedProfileId = selectedProfile?.id,
            cursorSpeed = (selectedProfile?.cursorSpeed ?: 1f) * 100f,
            overlayOpacity = preferences.getFloat("overlay_opacity", InputControlsView.DEFAULT_OVERLAY_OPACITY) * 100f,
            disableMouseInput = selectedProfile?.isDisableMouseInput ?: false,
            controllers = loadControllers(selectedProfile),
            isBusy = isBusy,
            message = message,
            downloadItems = downloadItems,
        )
    }

    private fun loadControllers(profile: ControlsProfile?): List<ExternalControllerUiState> {
        val controllers = profile?.loadControllers()?.toMutableList() ?: mutableListOf()
        for (controller in ExternalController.getControllers()) {
            if (!controllers.contains(controller)) {
                controllers.add(controller)
            }
        }
        return controllers.map {
            ExternalControllerUiState(
                id = it.id,
                name = it.name,
                bindingCount = it.controllerBindingCount,
                connected = it.isConnected,
            )
        }
    }

    private fun showMessage(message: String) {
        mutate { it.copy(message = message, isBusy = false) }
    }

    private fun mutate(block: (InputControlsUiState) -> InputControlsUiState) {
        _state.value = block(_state.value)
    }

    companion object {
        private const val INPUT_CONTROLS_URL = "https://raw.githubusercontent.com/brunodev85/winlator/main/input_controls/%s"
    }
}
