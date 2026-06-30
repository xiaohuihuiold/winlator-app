@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.shortcuts

import android.app.Application
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.winlator.XServerDisplayActivity
import com.winlator.container.AudioDrivers
import com.winlator.container.DXWrappers
import com.winlator.container.GraphicsDrivers
import com.winlator.container.Shortcut
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.R

@Composable
fun ShortcutsScreen() {
    val application = LocalContext.current.applicationContext as Application
    val context = LocalContext.current
    val navController = LocalNavController.current
    val viewModel: ShortcutsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ShortcutsViewModel(application) }
        },
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateFolder by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<Shortcut?>(null) }

    fun navigateBack() {
        if (!viewModel.goBack()) {
            if (!navController.popBackStack()) {
                navController.navigate("home") {
                    popUpTo("shortcuts") {
                        inclusive = true
                    }
                }
            }
        }
    }

    BackHandler {
        navigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    ShortcutsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = ::navigateBack,
        onCreateFolder = { showCreateFolder = true },
        onPaste = viewModel::pasteFiles,
        onToggleStyle = {
            val nextStyle = if (state.viewStyle == ShortcutViewStyle.GRID) {
                ShortcutViewStyle.LIST
            }
            else {
                ShortcutViewStyle.GRID
            }
            viewModel.setViewStyle(nextStyle)
        },
        onShortcutClick = { shortcut ->
            if (shortcut.file.isDirectory) {
                viewModel.openFolder(shortcut)
            }
            else {
                context.startActivity(
                    Intent(context, XServerDisplayActivity::class.java).apply {
                        putExtra("container_id", shortcut.container.id)
                        putExtra("shortcut_path", shortcut.file.path)
                    },
                )
            }
        },
        onAction = { shortcut, action ->
            when (action) {
                ShortcutAction.Settings -> viewModel.editShortcut(shortcut)
                ShortcutAction.Copy -> viewModel.copyShortcut(shortcut, cutMode = false)
                ShortcutAction.Cut -> viewModel.copyShortcut(shortcut, cutMode = true)
                ShortcutAction.Remove -> pendingRemoval = shortcut
            }
        },
    )

    if (showCreateFolder) {
        CreateFolderDialog(
            containers = state.containers,
            currentFolderContainerId = state.currentFolderContainerId,
            onDismiss = { showCreateFolder = false },
            onCreate = { containerId, name ->
                showCreateFolder = false
                viewModel.createFolder(containerId, name)
            },
        )
    }

    pendingRemoval?.let { shortcut ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove)) },
            text = { Text(stringResource(R.string.do_you_want_to_remove_this_file)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        viewModel.removeShortcut(shortcut)
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    state.settings?.let { settings ->
        ShortcutSettingsDialog(
            settings = settings,
            box64Presets = state.box64Presets,
            controlsProfiles = state.controlsProfiles,
            onDismiss = viewModel::dismissSettings,
            onSave = viewModel::saveSettings,
            onUpdate = viewModel::updateSettings,
        )
    }
}

@Composable
private fun ShortcutsContent(
    state: ShortcutsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCreateFolder: () -> Unit,
    onPaste: () -> Unit,
    onToggleStyle: () -> Unit,
    onShortcutClick: (Shortcut) -> Unit,
    onAction: (Shortcut, ShortcutAction) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onCreateFolder, enabled = state.currentFolderContainerId != null || state.containers.isNotEmpty()) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(R.string.new_folder))
                    }
                    IconButton(onClick = onPaste, enabled = state.canPaste) {
                        Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                    IconButton(onClick = onToggleStyle) {
                        val icon = if (state.viewStyle == ShortcutViewStyle.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView
                        Icon(icon, contentDescription = stringResource(R.string.view_style))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.shortcuts.isEmpty()) {
                EmptyShortcutsState(modifier = Modifier.align(Alignment.Center))
            }
            else if (state.viewStyle == ShortcutViewStyle.GRID) {
                ShortcutGrid(
                    shortcuts = state.shortcuts,
                    onShortcutClick = onShortcutClick,
                    onAction = onAction,
                )
            }
            else {
                ShortcutList(
                    shortcuts = state.shortcuts,
                    onShortcutClick = onShortcutClick,
                    onAction = onAction,
                )
            }

            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EmptyShortcutsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.empty_shortcuts_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.empty_shortcuts_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShortcutGrid(
    shortcuts: List<Shortcut>,
    onShortcutClick: (Shortcut) -> Unit,
    onAction: (Shortcut, ShortcutAction) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        items(shortcuts, key = { it.file.path }) { shortcut ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onShortcutClick(shortcut) },
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ShortcutIcon(
                        shortcut = shortcut,
                        modifier = Modifier
                            .size(68.dp)
                            .align(Alignment.Center),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shortcut.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = shortcut.container.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ShortcutActions(shortcut = shortcut, onAction = onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutList(
    shortcuts: List<Shortcut>,
    onShortcutClick: (Shortcut) -> Unit,
    onAction: (Shortcut, ShortcutAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(shortcuts, key = { it.file.path }) { shortcut ->
            ListItem(
                modifier = Modifier.clickable { onShortcutClick(shortcut) },
                leadingContent = {
                    ShortcutIcon(
                        shortcut = shortcut,
                        modifier = Modifier.size(42.dp),
                    )
                },
                headlineContent = {
                    Text(
                        text = shortcut.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = if (shortcut.file.isDirectory) shortcut.container.name else shortcut.path.orEmpty().ifBlank { shortcut.container.name },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onShortcutClick(shortcut) }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.run),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        ShortcutActions(shortcut = shortcut, onAction = onAction)
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ShortcutIcon(
    shortcut: Shortcut,
    modifier: Modifier = Modifier,
) {
    if (shortcut.icon != null) {
        Image(
            bitmap = shortcut.icon.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
    else {
        Image(
            painter = painterResource(if (shortcut.file.isDirectory) R.drawable.container_folder else R.drawable.container_file_link),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

private enum class ShortcutAction {
    Settings,
    Copy,
    Cut,
    Remove,
}

@Composable
private fun ShortcutActions(
    shortcut: Shortcut,
    onAction: (Shortcut, ShortcutAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (shortcut.file.isFile) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        expanded = false
                        onAction(shortcut, ShortcutAction.Settings)
                    },
                )
            }
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                text = { Text(stringResource(R.string.copy)) },
                onClick = {
                    expanded = false
                    onAction(shortcut, ShortcutAction.Copy)
                },
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                text = { Text(stringResource(R.string.cut)) },
                onClick = {
                    expanded = false
                    onAction(shortcut, ShortcutAction.Cut)
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                text = { Text(stringResource(R.string.remove)) },
                onClick = {
                    expanded = false
                    onAction(shortcut, ShortcutAction.Remove)
                },
            )
        }
    }
}

@Composable
private fun CreateFolderDialog(
    containers: List<ShortcutContainerOption>,
    currentFolderContainerId: Int?,
    onDismiss: () -> Unit,
    onCreate: (Int?, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedContainerId by remember(currentFolderContainerId, containers) {
        mutableStateOf(currentFolderContainerId ?: containers.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_folder)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (currentFolderContainerId == null) {
                    if (containers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_items_to_display),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else {
                        DropdownField(
                            label = stringResource(R.string.container),
                            options = containers.map { ShortcutOption(it.id.toString(), it.name) },
                            selectedValue = selectedContainerId?.toString().orEmpty(),
                            onValueChange = { selectedContainerId = it.toIntOrNull() },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(selectedContainerId, name) },
                enabled = name.isNotBlank() && (currentFolderContainerId != null || selectedContainerId != null),
            ) {
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
private fun ShortcutSettingsDialog(
    settings: ShortcutSettingsUiState,
    box64Presets: List<ShortcutOption>,
    controlsProfiles: List<ShortcutOption>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: ((ShortcutSettingsUiState) -> ShortcutSettingsUiState) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = settings.shortcut.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SectionTitle(stringResource(R.string.general))
                    OutlinedTextField(
                        value = settings.name,
                        onValueChange = { value -> onUpdate { it.copy(name = value, errorMessage = null) } },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (settings.errorMessage != null) {
                        Text(
                            text = settings.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.execArgs,
                        onValueChange = { value -> onUpdate { it.copy(execArgs = value) } },
                        label = { Text(stringResource(R.string.exec_arguments)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.screen_size))
                    DropdownField(
                        label = stringResource(R.string.screen_size),
                        options = screenSizeOptions(),
                        selectedValue = settings.screenSize,
                        onValueChange = { value -> onUpdate { it.copy(screenSize = value) } },
                    )
                    if (settings.screenSize == ShortcutsViewModel.CUSTOM_SCREEN_SIZE) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = settings.customWidth,
                                onValueChange = { value -> onUpdate { it.copy(customWidth = value.filter(Char::isDigit)) } },
                                label = { Text(stringResource(R.string.width)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = settings.customHeight,
                                onValueChange = { value -> onUpdate { it.copy(customHeight = value.filter(Char::isDigit)) } },
                                label = { Text(stringResource(R.string.height)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.graphics_driver))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DropdownField(
                            label = "Vulkan",
                            options = listOf(
                                ShortcutOption(GraphicsDrivers.TURNIP, GraphicsDrivers.getName(GraphicsDrivers.TURNIP)),
                                ShortcutOption(GraphicsDrivers.VORTEK, GraphicsDrivers.getName(GraphicsDrivers.VORTEK)),
                            ),
                            selectedValue = settings.vulkanDriver,
                            onValueChange = { value -> onUpdate { it.copy(vulkanDriver = value) } },
                            modifier = Modifier.weight(1f),
                        )
                        DropdownField(
                            label = "OpenGL",
                            options = listOf(
                                ShortcutOption(GraphicsDrivers.ZINK, GraphicsDrivers.getName(GraphicsDrivers.ZINK)),
                                ShortcutOption(GraphicsDrivers.VIRGL, GraphicsDrivers.getName(GraphicsDrivers.VIRGL)),
                                ShortcutOption(GraphicsDrivers.GLADIO, GraphicsDrivers.getName(GraphicsDrivers.GLADIO)),
                            ),
                            selectedValue = settings.openglDriver,
                            onValueChange = { value -> onUpdate { it.copy(openglDriver = value) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.graphicsDriverConfig,
                        onValueChange = { value -> onUpdate { it.copy(graphicsDriverConfig = value) } },
                        label = { Text(stringResource(R.string.configuration)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.dxwrapper))
                    DropdownField(
                        label = stringResource(R.string.dxwrapper),
                        options = listOf(
                            ShortcutOption(DXWrappers.WINED3D, DXWrappers.getName(DXWrappers.WINED3D)),
                            ShortcutOption(DXWrappers.DXVK, DXWrappers.getName(DXWrappers.DXVK)),
                        ),
                        selectedValue = settings.dxWrapper,
                        onValueChange = { value -> onUpdate { it.copy(dxWrapper = value) } },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.dxWrapperConfig,
                        onValueChange = { value -> onUpdate { it.copy(dxWrapperConfig = value) } },
                        label = { Text(stringResource(R.string.configuration)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.audio))
                    DropdownField(
                        label = stringResource(R.string.audio_driver),
                        options = listOf(
                            ShortcutOption(AudioDrivers.ALSA, "ALSA"),
                            ShortcutOption(AudioDrivers.PULSEAUDIO, "PulseAudio"),
                        ),
                        selectedValue = settings.audioDriver,
                        onValueChange = { value -> onUpdate { it.copy(audioDriver = value) } },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.audioDriverConfig,
                        onValueChange = { value -> onUpdate { it.copy(audioDriverConfig = value) } },
                        label = { Text(stringResource(R.string.configuration)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SettingsSwitch(
                        title = stringResource(R.string.force_fullscreen),
                        checked = settings.forceFullscreen,
                        onCheckedChange = { checked -> onUpdate { it.copy(forceFullscreen = checked) } },
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.controls_profile))
                    DropdownField(
                        label = stringResource(R.string.controls_profile),
                        options = controlsProfiles,
                        selectedValue = settings.controlsProfile,
                        onValueChange = { value -> onUpdate { it.copy(controlsProfile = value) } },
                    )
                    Spacer(Modifier.height(12.dp))
                    DropdownField(
                        label = stringResource(R.string.directinput_mapper_type),
                        options = indexedOptions(stringArrayResource(R.array.dinput_mapper_type_entries)),
                        selectedValue = settings.dinputMapperType.toString(),
                        onValueChange = { value -> onUpdate { it.copy(dinputMapperType = value.toIntOrNull() ?: 1) } },
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.box64))
                    DropdownField(
                        label = stringResource(R.string.box64_preset),
                        options = box64Presets,
                        selectedValue = settings.box64Preset,
                        onValueChange = { value -> onUpdate { it.copy(box64Preset = value) } },
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.advanced))
                    OutlinedTextField(
                        value = settings.envVars,
                        onValueChange = { value -> onUpdate { it.copy(envVars = value) } },
                        label = { Text(stringResource(R.string.environment_variables)) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.winComponents,
                        onValueChange = { value -> onUpdate { it.copy(winComponents = value) } },
                        label = { Text(stringResource(R.string.win_components)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
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
private fun SectionTitle(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider()
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DropdownField(
    label: String,
    options: List<ShortcutOption>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label
        ?: options.firstOrNull()?.label
        ?: selectedValue

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onValueChange(option.value)
                    },
                )
            }
        }
    }
}

@Composable
private fun screenSizeOptions(): List<ShortcutOption> {
    val labels = stringArrayResource(R.array.screen_size_entries)
    return labels.mapIndexed { index, label ->
        val value = if (index == 0) {
            ShortcutsViewModel.CUSTOM_SCREEN_SIZE
        }
        else {
            label.substringBefore(" ")
        }
        ShortcutOption(value, label)
    }
}

private fun indexedOptions(labels: Array<String>): List<ShortcutOption> {
    return labels.mapIndexed { index, label -> ShortcutOption(index.toString(), label) }
}
