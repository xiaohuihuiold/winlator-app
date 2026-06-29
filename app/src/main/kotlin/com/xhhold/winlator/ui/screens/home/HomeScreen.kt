@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.winlator.WinlatorActivity
import com.winlator.container.Container
import com.winlator.core.StringUtils
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.LocalMainModel
import com.xhhold.winlator.R
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val mainViewModel = LocalMainModel.current

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val containers by mainViewModel.containers.collectAsState()
    val isRefreshing by mainViewModel.isRefreshing.collectAsState()
    val storageInfo by homeViewModel.storageInfo.collectAsState()
    val viewStyle by homeViewModel.viewStyle.collectAsState()
    var pendingAction by remember { mutableStateOf<PendingContainerAction?>(null) }

    val onContainerAction: (Container, ContainerCardAction) -> Unit = { container, action ->
        when (action) {
            ContainerCardAction.Run -> homeViewModel.runContainer(
                activity, container.id
            )

            ContainerCardAction.FileManager -> navController.navigate("file-manager/${container.id}")

            ContainerCardAction.Edit -> navController.navigate("detail/${container.id}")

            ContainerCardAction.Copy -> pendingAction = PendingContainerAction(
                container,
                ContainerCardAction.Copy,
            )

            ContainerCardAction.Remove -> pendingAction = PendingContainerAction(
                container,
                ContainerCardAction.Remove,
            )

            ContainerCardAction.Info -> homeViewModel.loadStorageInfo(container.id)
        }
    }

    CompositionLocalProvider(
        LocalHomeViewModel provides homeViewModel
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HomeDrawer(
                    onContainersClick = {
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("show_shortcuts_first", false).apply()
                        scope.launch { drawerState.close() }
                    },
                    onShortcutsClick = {
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("show_shortcuts_first", true).apply()
                        scope.launch { drawerState.close() }
                        navController.navigate("shortcuts") {
                            launchSingleTop = true
                        }
                    },
                    onInputControlsClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("input-controls") {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    onAboutClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("about") {
                            launchSingleTop = true
                        }
                    },
                    onLegacyUiClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, WinlatorActivity::class.java))
                        activity?.finish()
                    },
                )
            },
        ) {
            Scaffold(
                topBar = {
                    HomeTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAddClick = { navController.navigate("detail/0") },
                        viewStyle = viewStyle,
                        onToggleStyle = {
                            homeViewModel.setViewStyle(
                                if (viewStyle == HomeViewStyle.GRID) HomeViewStyle.LIST else HomeViewStyle.GRID,
                            )
                        },
                    )
                }) { innerPadding ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = mainViewModel::refreshContainers,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (containers.isEmpty()) {
                        HomeEmptyList()
                    }
                    else if (viewStyle == HomeViewStyle.GRID) {
                        HomeContainerGrid(containers = containers, onItemClick = { container ->
                            navController.navigate("detail/${container.id}")
                        }, onActionClick = onContainerAction)
                    }
                    else {
                        HomeContainerList(containers = containers, onItemClick = { container ->
                            navController.navigate("detail/${container.id}")
                        }, onActionClick = onContainerAction)
                    }
                }
            }
        }

        pendingAction?.let { request ->
            ContainerActionDialog(
                request = request,
                onDismiss = { pendingAction = null },
                onConfirm = {
                    pendingAction = null
                    when (request.action) {
                        ContainerCardAction.Copy -> homeViewModel.duplicateContainer(request.container.id) {
                            mainViewModel.refreshContainers()
                        }

                        ContainerCardAction.Remove -> homeViewModel.removeContainer(request.container.id) {
                            mainViewModel.refreshContainers()
                        }

                        else -> Unit
                    }
                },
            )
        }

        storageInfo?.let { info ->
            StorageInfoDialog(
                info = info,
                onDismiss = homeViewModel::dismissStorageInfo,
                onClearCache = { homeViewModel.clearStorageCache(info.containerId) },
            )
        }
    }
}

private data class PendingContainerAction(
    val container: Container,
    val action: ContainerCardAction,
)

@Composable
private fun ContainerActionDialog(
    request: PendingContainerAction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isRemove = request.action == ContainerCardAction.Remove
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isRemove) R.string.remove else R.string.duplicate))
        },
        text = {
            Text(stringResource(if (isRemove) R.string.do_you_want_to_remove_this_container else R.string.do_you_want_to_duplicate_this_container))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun StorageInfoDialog(
    info: StorageInfoUiState,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.storage_info))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = info.containerName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider()
                if (info.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.loading))
                    }
                }
                else {
                    StorageMetric(label = stringResource(R.string.drive_c), value = StringUtils.formatBytes(info.driveCSize))
                    StorageMetric(label = stringResource(R.string.cache), value = StringUtils.formatBytes(info.cacheSize))
                    StorageMetric(label = stringResource(R.string.total), value = StringUtils.formatBytes(info.totalSize))
                    val usedPercent = if (info.internalStorageSize > 0) {
                        ((info.totalSize.toDouble() / info.internalStorageSize.toDouble()) * 100).toInt()
                            .coerceIn(0, 100)
                    }
                    else {
                        0
                    }
                    LinearProgressIndicator(
                        progress = { usedPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "$usedPercent% ${stringResource(R.string.estimated_used_space)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            if (!info.isLoading) {
                TextButton(onClick = onClearCache) {
                    Text(stringResource(R.string.clear_cache))
                }
            }
        },
    )
}

@Composable
private fun StorageMetric(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HomeEmptyList() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_items_to_display),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun HomeDrawer(
    onContainersClick: () -> Unit = {},
    onShortcutsClick: () -> Unit = {},
    onInputControlsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLegacyUiClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .width(240.dp)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        ListItem(modifier = Modifier.clickable {
            onContainersClick()
        }, headlineContent = {
            Text(stringResource(R.string.containers))
        }, leadingContent = {
            Icon(
                Icons.Default.Dns,
                contentDescription = stringResource(R.string.containers),
                modifier = Modifier.size(24.dp)
            )
        })
        ListItem(modifier = Modifier.clickable {
            onShortcutsClick()
        }, headlineContent = {
            Text(stringResource(R.string.shortcuts))
        }, leadingContent = {
            Icon(
                Icons.Default.Apps,
                contentDescription = stringResource(R.string.shortcuts),
                modifier = Modifier.size(24.dp)
            )
        })
        ListItem(modifier = Modifier.clickable {
            onInputControlsClick()
        }, headlineContent = {
            Text(stringResource(R.string.input_controls))
        }, leadingContent = {
            Icon(
                Icons.Default.Gamepad,
                contentDescription = stringResource(R.string.input_controls),
                modifier = Modifier.size(24.dp)
            )
        })
        ListItem(modifier = Modifier.clickable {
            onSettingsClick()
        }, headlineContent = {
            Text(stringResource(R.string.settings))
        }, leadingContent = {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.size(24.dp)
            )
        })
        ListItem(modifier = Modifier.clickable {
            onAboutClick()
        }, headlineContent = {
            Text(stringResource(R.string.about))
        }, leadingContent = {
            Icon(
                Icons.Default.Info,
                contentDescription = stringResource(R.string.about),
                modifier = Modifier.size(24.dp)
            )
        })
        ListItem(modifier = Modifier.clickable {
            onLegacyUiClick()
        }, headlineContent = {
            Text(stringResource(R.string.classic_view))
        }, leadingContent = {
            Icon(
                Icons.Default.Apps,
                contentDescription = stringResource(R.string.classic_view),
                modifier = Modifier.size(24.dp)
            )
        })
    }
}

@Composable
fun HomeTopBar(
    onMenuClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    viewStyle: HomeViewStyle = HomeViewStyle.GRID,
    onToggleStyle: () -> Unit = {},
) {
    TopAppBar(title = { Text(stringResource(R.string.app_name)) }, navigationIcon = {
        IconButton(onClick = onMenuClick) {
            Icon(
                Icons.Default.Menu, contentDescription = stringResource(R.string.menu)
            )
        }
    }, actions = {
        IconButton(onClick = onToggleStyle) {
            val icon = if (viewStyle == HomeViewStyle.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView
            val description = if (viewStyle == HomeViewStyle.GRID) R.string.list_view else R.string.grid_view
            Icon(icon, contentDescription = stringResource(description))
        }
        IconButton(onClick = onAddClick) {
            Icon(
                Icons.Default.Add, contentDescription = stringResource(R.string.add)
            )
        }
    })
}

@Composable
fun HomeContainerGrid(
    modifier: Modifier = Modifier,
    containers: List<Container>,
    onItemClick: (Container) -> Unit = {},
    onActionClick: (Container, ContainerCardAction) -> Unit = { _, _ -> },
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(containers, key = { it.id }) { container ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onItemClick(container) },
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.Center)
                            .alpha(0.25f)
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = container.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        ContainerCardActions(
                            onActionClick = { action ->
                                onActionClick(container, action)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContainerList(
    modifier: Modifier = Modifier,
    containers: List<Container>,
    onItemClick: (Container) -> Unit = {},
    onActionClick: (Container, ContainerCardAction) -> Unit = { _, _ -> },
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(containers, key = { it.id }) { container ->
            ListItem(
                modifier = Modifier.clickable { onItemClick(container) },
                leadingContent = {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                    )
                },
                headlineContent = {
                    Text(
                        text = container.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = "${container.screenSize} • ${container.wineVersion}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onActionClick(container, ContainerCardAction.Run) }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.run),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        ContainerCardActions(
                            onActionClick = { action -> onActionClick(container, action) },
                        )
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

enum class ContainerCardAction {
    Run, FileManager, Edit, Copy, Remove, Info
}

@Composable
fun ContainerCardActions(
    modifier: Modifier = Modifier,
    onActionClick: (ContainerCardAction) -> Unit = {},
) {
    val menuExpanded = remember { mutableStateOf(false) }
    IconButton(
        onClick = { menuExpanded.value = true },
        modifier = CombinedModifier(outer = modifier, inner = Modifier.size(24.dp))
    ) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more),
            modifier = Modifier.size(16.dp)
        )
        DropdownMenu(
            expanded = menuExpanded.value, onDismissRequest = { menuExpanded.value = false }) {
            ContainerMenuItem(
                icon = Icons.Default.PlayArrow,
                iconTint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(R.string.run),
                text = stringResource(R.string.run),
                onClick = {
                    menuExpanded.value = false
                    onActionClick(ContainerCardAction.Run)
                })
            ContainerMenuItem(
                icon = Icons.Default.FolderOpen,
                contentDescription = stringResource(R.string.file_manager),
                text = stringResource(R.string.file_manager),
                onClick = {
                    menuExpanded.value = false
                    onActionClick(ContainerCardAction.FileManager)
                })
            ContainerMenuItem(
                icon = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit),
                text = stringResource(R.string.edit),
                onClick = {
                    menuExpanded.value = false
                    onActionClick(ContainerCardAction.Edit)
                })
            ContainerMenuItem(
                icon = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.duplicate),
                text = stringResource(R.string.duplicate),
                onClick = {
                    menuExpanded.value = false
                    onActionClick(ContainerCardAction.Copy)
                })
            ContainerMenuItem(
                icon = Icons.Default.Delete,
                iconTint = MaterialTheme.colorScheme.error,
                contentDescription = stringResource(R.string.remove),
                text = stringResource(R.string.remove),
                onClick = {
                    menuExpanded.value = false
                    onActionClick(ContainerCardAction.Remove)
                })
            ContainerMenuItem(
                icon = Icons.Default.Info,
                contentDescription = stringResource(R.string.storage_info),
                text = stringResource(R.string.storage_info),
                onClick = {
                    menuExpanded.value = false
                    onActionClick(ContainerCardAction.Info)
                })
        }
    }
}

@Composable
private fun ContainerMenuItem(
    icon: ImageVector,
    iconTint: Color = LocalContentColor.current,
    contentDescription: String,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                icon, tint = iconTint, contentDescription = contentDescription
            )
        }, text = { Text(text) }, onClick = onClick
    )
}
