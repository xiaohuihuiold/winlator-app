package com.xhhold.winlator.ui.screens.home

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.winlator.XServerDisplayActivity
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.core.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.file.Files
import java.util.ArrayDeque
import java.util.concurrent.Executors

val LocalHomeViewModel = compositionLocalOf<HomeViewModel> {
    error("HomeViewModel not provided")
}

data class StorageInfoUiState(
    val containerId: Int,
    val containerName: String,
    val driveCSize: Long = 0,
    val cacheSize: Long = 0,
    val totalSize: Long = 0,
    val internalStorageSize: Long = 0,
    val isLoading: Boolean = true,
)

enum class HomeViewStyle {
    GRID,
    LIST,
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _storageInfo = MutableStateFlow<StorageInfoUiState?>(null)
    val storageInfo: StateFlow<StorageInfoUiState?> = _storageInfo

    private val _viewStyle = MutableStateFlow(
        runCatching {
            HomeViewStyle.valueOf(preferences.getString("containers_view_style", HomeViewStyle.GRID.name) ?: HomeViewStyle.GRID.name)
        }.getOrDefault(HomeViewStyle.GRID),
    )
    val viewStyle: StateFlow<HomeViewStyle> = _viewStyle

    fun setViewStyle(viewStyle: HomeViewStyle) {
        preferences.edit().putString("containers_view_style", viewStyle.name).apply()
        _viewStyle.value = viewStyle
    }

    fun runContainer(activity: Activity?, containerId: Int) {
        if (activity == null) {
            throw IllegalArgumentException("Activity cannot be null")
        }
        val intent = Intent(activity, XServerDisplayActivity::class.java)
        intent.putExtra("container_id", containerId)
        activity.startActivity(intent)
    }

    fun duplicateContainer(containerId: Int, onComplete: () -> Unit) {
        val manager = ContainerManager(getApplication())
        val container = manager.getContainerById(containerId) ?: return
        manager.duplicateContainerAsync(container) {
            onComplete()
        }
    }

    fun removeContainer(containerId: Int, onComplete: () -> Unit) {
        val manager = ContainerManager(getApplication())
        val container = manager.getContainerById(containerId) ?: return
        manager.removeContainerAsync(container) {
            onComplete()
        }
    }

    fun loadStorageInfo(containerId: Int) {
        val manager = ContainerManager(getApplication())
        val container = manager.getContainerById(containerId) ?: return
        _storageInfo.value = StorageInfoUiState(
            containerId = container.id,
            containerName = container.name,
            internalStorageSize = FileUtils.getInternalStorageSize(),
            isLoading = true,
        )

        executor.execute {
            val storageInfo = calculateStorageInfo(container)
            mainHandler.post {
                _storageInfo.value = storageInfo
            }
        }
    }

    fun dismissStorageInfo() {
        _storageInfo.value = null
    }

    fun clearStorageCache(containerId: Int) {
        val app = getApplication<Application>()
        _storageInfo.value = _storageInfo.value?.copy(isLoading = true)
        executor.execute {
            val manager = ContainerManager(app)
            val container = manager.getContainerById(containerId) ?: return@execute
            FileUtils.clear(File(container.rootDir, ".cache"))
            FileUtils.clear(app.cacheDir)
            container.putExtra("desktopTheme", null)
            container.saveData()
            val storageInfo = calculateStorageInfo(container)
            mainHandler.post {
                _storageInfo.value = storageInfo
            }
        }
    }

    private fun calculateStorageInfo(container: Container): StorageInfoUiState {
        val rootDir = container.rootDir
        val driveCSize = directorySize(File(rootDir, ".wine/drive_c"))
        val cacheSize = directorySize(File(rootDir, ".cache")) + directorySize(getApplication<Application>().cacheDir)
        val totalSize = driveCSize + cacheSize
        return StorageInfoUiState(
            containerId = container.id,
            containerName = container.name,
            driveCSize = driveCSize,
            cacheSize = cacheSize,
            totalSize = totalSize,
            internalStorageSize = FileUtils.getInternalStorageSize(),
            isLoading = false,
        )
    }

    private fun directorySize(root: File): Long {
        if (!root.exists()) return 0
        var total = 0L
        val stack = ArrayDeque<File>()
        stack.add(root)
        while (!stack.isEmpty()) {
            val file = stack.removeLast()
            if (Files.isSymbolicLink(file.toPath())) continue
            if (file.isDirectory) {
                file.listFiles()?.forEach(stack::add)
            }
            else {
                total += file.length().coerceAtLeast(0L)
            }
        }
        return total
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}
