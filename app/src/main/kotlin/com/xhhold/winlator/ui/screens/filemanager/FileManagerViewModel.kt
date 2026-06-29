package com.xhhold.winlator.ui.screens.filemanager

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.container.FileInfo
import com.winlator.core.FileUtils
import com.winlator.core.StringUtils
import com.winlator.core.WineUtils
import com.winlator.win32.MSLink
import com.xhhold.winlator.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

enum class FileManagerViewStyle {
    LIST,
    GRID,
}

data class FileInfoDetails(
    val name: String,
    val type: Int,
    val location: String,
    val modified: String,
    val size: Long,
    val itemCount: Int,
)

data class FileManagerUiState(
    val exists: Boolean = true,
    val title: String = "",
    val files: List<FileInfo> = emptyList(),
    val viewStyle: FileManagerViewStyle = FileManagerViewStyle.GRID,
    val canGoBack: Boolean = false,
    val canPaste: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
    val details: FileInfoDetails? = null,
)

class FileManagerViewModel(
    application: Application,
    private val containerId: Int,
) : AndroidViewModel(application) {
    private val context = application
    private val manager = ContainerManager(context)
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val container: Container? = manager.getContainerById(containerId)
    private val folderStack = mutableListOf<FileInfo>()
    private var clipboard: FileClipboard? = null

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<FileManagerUiState> = _state

    fun refresh() {
        refreshContent()
    }

    fun setViewStyle(viewStyle: FileManagerViewStyle) {
        preferences.edit().putString("container_file_manager_view_style", viewStyle.name).apply()
        mutate { it.copy(viewStyle = viewStyle) }
    }

    fun openFolder(file: FileInfo) {
        folderStack.add(file)
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

    fun goHome() {
        clearClipboard(emit = false)
        folderStack.clear()
        refreshContent()
    }

    fun createFolder(name: String) {
        val cleanName = StringUtils.clearReservedChars(name.trim())
        val parent = folderStack.lastOrNull()?.toFile() ?: return
        if (cleanName.isEmpty()) return

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

    fun copyFile(file: FileInfo, cutMode: Boolean) {
        clipboard = FileClipboard(File(file.path), cutMode)
        mutate { it.copy(canPaste = true) }
    }

    fun pasteFiles() {
        val currentClipboard = clipboard ?: return
        val targetDir = folderStack.lastOrNull()?.toFile()
        if (targetDir == null) {
            clearClipboard(emit = false)
            showMessage(context.getString(R.string.you_cannot_paste_files_here))
            return
        }
        val targetFile = File(targetDir, currentClipboard.file.name)
        if (targetFile.exists()) {
            showMessage(context.getString(R.string.there_already_file_with_that_name))
            return
        }

        mutate { it.copy(isBusy = true) }
        executor.execute {
            if (FileUtils.copy(currentClipboard.file, targetFile) && currentClipboard.cutMode) {
                FileUtils.delete(currentClipboard.file)
            }
            handler.post {
                clipboard = null
                refreshContent(isBusy = false)
            }
        }
    }

    fun removeFile(file: FileInfo) {
        mutate { it.copy(isBusy = true) }
        executor.execute {
            FileUtils.delete(file.toFile())
            handler.post {
                clearClipboard(emit = false)
                refreshContent(isBusy = false)
            }
        }
    }

    fun renameFile(file: FileInfo, newName: String) {
        if (newName.isBlank()) return
        clearClipboard(emit = false)
        if (!file.renameTo(newName)) {
            showMessage(context.getString(R.string.there_already_file_with_that_name))
        }
        else {
            refreshContent()
        }
    }

    fun addFavorite(file: FileInfo) {
        val targetContainer = container ?: return
        val favoritesDir = File(targetContainer.userDir, context.getString(R.string.favorites))
        if (!favoritesDir.isDirectory) favoritesDir.mkdirs()
        val targetFile = File(favoritesDir, "${FileUtils.getBasename(file.name)}.lnk")
        if (targetFile.exists()) return

        val linkInfo = MSLink.LinkInfo().apply {
            targetPath = WineUtils.unixToDOSPath(file.path, targetContainer)
            isDirectory = effectiveType(file) == FileInfo.Type.DIRECTORY
        }
        if (MSLink.createFile(linkInfo, targetFile)) {
            showMessage(context.getString(R.string.file_added_to_favorites))
        }
    }

    fun showInfo(file: FileInfo) {
        val targetContainer = container ?: return
        val targetFile = file.toFile()
        val modified = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(targetFile.lastModified()))
        mutate {
            it.copy(
                details = FileInfoDetails(
                    name = file.displayName,
                    type = if (effectiveType(file) == FileInfo.Type.DIRECTORY) R.string.folder else R.string.file,
                    location = WineUtils.unixToDOSPath(FileUtils.getDirname(file.path), targetContainer),
                    modified = modified,
                    size = file.size,
                    itemCount = file.itemCount,
                ),
            )
        }
    }

    fun dismissInfo() {
        mutate { it.copy(details = null) }
    }

    fun clearMessage() {
        mutate { it.copy(message = null) }
    }

    fun isRunnableFile(file: FileInfo): Boolean {
        return effectiveType(file) == FileInfo.Type.FILE
    }

    fun effectiveType(file: FileInfo): FileInfo.Type {
        val linkInfo = file.getLinkinfo()
        return if (linkInfo != null && linkInfo.isDirectory) FileInfo.Type.DIRECTORY else file.type
    }

    fun currentContainerId(): Int = containerId

    private fun createInitialState(): FileManagerUiState {
        val viewStyle = runCatching {
            FileManagerViewStyle.valueOf(
                preferences.getString("container_file_manager_view_style", FileManagerViewStyle.GRID.name)
                    ?: FileManagerViewStyle.GRID.name,
            )
        }.getOrDefault(FileManagerViewStyle.GRID)

        val target = container ?: return FileManagerUiState(exists = false, viewStyle = viewStyle)
        return FileManagerUiState(
            title = target.name,
            files = manager.loadFiles(target, null),
            viewStyle = viewStyle,
        )
    }

    private fun refreshContent(isBusy: Boolean = state.value.isBusy) {
        val target = container ?: return mutate { it.copy(exists = false) }
        val parent = folderStack.lastOrNull()
        mutate {
            it.copy(
                title = if (parent != null) currentWorkingPath() else target.name,
                files = manager.loadFiles(target, parent),
                canGoBack = parent != null,
                canPaste = clipboard != null,
                isBusy = isBusy,
            )
        }
    }

    private fun currentWorkingPath(): String {
        if (folderStack.isEmpty()) return ""
        val value = folderStack.joinToString("\\") { it.displayName }
        return if (folderStack.size == 1) "$value\\" else value
    }

    private fun clearClipboard(emit: Boolean) {
        clipboard = null
        if (emit) mutate { it.copy(canPaste = false) }
    }

    private fun showMessage(message: String) {
        mutate { it.copy(message = message, isBusy = false, canPaste = clipboard != null) }
    }

    private fun mutate(block: (FileManagerUiState) -> FileManagerUiState) {
        _state.value = block(_state.value)
    }

    private data class FileClipboard(
        val file: File,
        val cutMode: Boolean,
    )
}
