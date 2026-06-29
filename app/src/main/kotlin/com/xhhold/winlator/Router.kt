package com.xhhold.winlator

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.xhhold.winlator.ui.screens.about.AboutScreen
import com.xhhold.winlator.ui.screens.detail.DetailScreen
import com.xhhold.winlator.ui.screens.filemanager.FileManagerScreen
import com.xhhold.winlator.ui.screens.home.HomeScreen
import com.xhhold.winlator.ui.screens.inputcontrols.InputControlsScreen
import com.xhhold.winlator.ui.screens.settings.SettingsScreen
import com.xhhold.winlator.ui.screens.shortcuts.ShortcutsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("NavController not provided")
}

val LocalMainModel = staticCompositionLocalOf<MainModel> {
    error("MainModel not provided")
}

@Composable
fun Nav(viewModel: MainModel = viewModel()) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val startDestination = if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_shortcuts_first", false)) {
        "shortcuts"
    }
    else {
        "home"
    }

    LaunchedEffect(Unit) {
        viewModel.refreshContainers()
    }

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalMainModel provides viewModel
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("home") {
                HomeScreen()
            }
            composable("shortcuts") {
                ShortcutsScreen()
            }
            composable("input-controls") {
                InputControlsScreen()
            }
            composable("file-manager/{containerId}") { backStackEntry ->
                val containerId = backStackEntry.arguments?.getString("containerId")?.toIntOrNull()
                FileManagerScreen(containerId = containerId)
            }
            composable("detail/{containerId}") { backStackEntry ->
                val containerId = backStackEntry.arguments?.getString("containerId")?.toIntOrNull()
                DetailScreen(containerId = containerId)
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("about") {
                AboutScreen()
            }
        }
    }

}

fun <T> MutableStateFlow<T>.readOnly(): StateFlow<T> {
    return this
}

class MainModel(application: Application) : AndroidViewModel(application) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _containers = MutableStateFlow(emptyList<Container>())
    val containers = _containers.readOnly()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.readOnly()

    fun refreshContainers() {
        _isRefreshing.value = true
        val application = getApplication<Application>()
        executor.execute {
            val result = runCatching {
                ContainerManager(application).containers.toList()
            }
            mainHandler.post {
                result.getOrNull()?.let { _containers.value = it }
                _isRefreshing.value = false
            }
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }

}
