@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.detail

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.winlator.container.AudioDrivers
import com.winlator.container.Container
import com.winlator.container.DXWrappers
import com.winlator.container.GraphicsDrivers
import com.winlator.core.FileUtils
import com.xhhold.winlator.LocalMainModel
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.R

@Composable
fun DetailScreen(
    containerId: Int? = null,
) {
    val application = LocalContext.current.applicationContext as Application
    val navController = LocalNavController.current
    val mainModel = LocalMainModel.current
    val detailViewModel: DetailViewModel = viewModel(
        key = "detail-${containerId ?: 0}",
        factory = viewModelFactory {
            initializer {
                DetailViewModel(application, containerId)
            }
        }
    )
    val state by detailViewModel.state.collectAsState()
    var pendingDrivePathIndex by remember { mutableStateOf<Int?>(null) }
    val openDrivePathLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val targetIndex = pendingDrivePathIndex
        pendingDrivePathIndex = null
        if (uri != null && targetIndex != null) {
            FileUtils.getFilePathFromUri(uri)?.let { path ->
                detailViewModel.updateDrivePath(targetIndex, path)
            }
        }
    }

    DetailContent(
        state = state,
        viewModel = detailViewModel,
        onBack = { navController.popBackStack() },
        onSave = {
            detailViewModel.save {
                mainModel.refreshContainers()
                navController.popBackStack()
            }
        },
        onBrowseDrivePath = { index ->
            pendingDrivePathIndex = index
            openDrivePathLauncher.launch(null)
        },
    )
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onBrowseDrivePath: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (state.isEditMode) R.string.edit_container else R.string.new_container),
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
                    IconButton(onClick = onSave, enabled = state.exists && !state.isSaving) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .height(22.dp)
                                    .width(22.dp),
                            )
                        }
                        else {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.ok))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!state.exists) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_items_to_display),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cancel))
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.general))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            item {
                SectionTitle(stringResource(R.string.screen_size))
                val screenOptions = screenSizeOptions()
                DropdownField(
                    label = stringResource(R.string.screen_size),
                    options = screenOptions,
                    selectedValue = state.screenSize,
                    onValueChange = viewModel::setScreenSize,
                )
                if (state.screenSize == DetailViewModel.CUSTOM_SCREEN_SIZE) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.customWidth,
                            onValueChange = viewModel::setCustomWidth,
                            label = { Text(stringResource(R.string.width)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.customHeight,
                            onValueChange = viewModel::setCustomHeight,
                            label = { Text(stringResource(R.string.height)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.wine_configuration))
                DropdownField(
                    label = stringResource(R.string.wine_version),
                    options = state.wineVersions,
                    selectedValue = state.wineVersion,
                    enabled = !state.isEditMode,
                    onValueChange = viewModel::setWineVersion,
                )
                Spacer(Modifier.height(12.dp))
                DropdownField(
                    label = stringResource(R.string.startup_selection),
                    options = indexedOptions(stringArrayResource(R.array.startup_selection_entries)),
                    selectedValue = state.startupSelection.toString(),
                    onValueChange = { viewModel.setStartupSelection(it.toInt()) },
                )
            }

            item {
                SectionTitle(stringResource(R.string.graphics_driver))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(
                        label = "Vulkan",
                        options = listOf(
                            DetailOption(GraphicsDrivers.TURNIP, GraphicsDrivers.getName(GraphicsDrivers.TURNIP)),
                            DetailOption(GraphicsDrivers.VORTEK, GraphicsDrivers.getName(GraphicsDrivers.VORTEK)),
                        ),
                        selectedValue = state.vulkanDriver,
                        onValueChange = viewModel::setVulkanDriver,
                        modifier = Modifier.weight(1f),
                    )
                    DropdownField(
                        label = "OpenGL",
                        options = listOf(
                            DetailOption(GraphicsDrivers.ZINK, GraphicsDrivers.getName(GraphicsDrivers.ZINK)),
                            DetailOption(GraphicsDrivers.VIRGL, GraphicsDrivers.getName(GraphicsDrivers.VIRGL)),
                            DetailOption(GraphicsDrivers.GLADIO, GraphicsDrivers.getName(GraphicsDrivers.GLADIO)),
                        ),
                        selectedValue = state.openglDriver,
                        onValueChange = viewModel::setOpenGLDriver,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                DropdownField(
                    label = stringResource(R.string.dxwrapper),
                    options = listOf(
                        DetailOption(DXWrappers.WINED3D, DXWrappers.getName(DXWrappers.WINED3D)),
                        DetailOption(DXWrappers.DXVK, DXWrappers.getName(DXWrappers.DXVK)),
                    ),
                    selectedValue = state.dxWrapper,
                    onValueChange = viewModel::setDXWrapper,
                )
            }

            item {
                SectionTitle(stringResource(R.string.audio_driver))
                DropdownField(
                    label = stringResource(R.string.audio_driver),
                    options = listOf(
                        DetailOption(AudioDrivers.ALSA, "ALSA"),
                        DetailOption(AudioDrivers.PULSEAUDIO, "PulseAudio"),
                    ),
                    selectedValue = state.audioDriver,
                    onValueChange = viewModel::setAudioDriver,
                )
                Spacer(Modifier.height(12.dp))
                DropdownField(
                    label = stringResource(R.string.hud_mode),
                    options = indexedOptions(stringArrayResource(R.array.hud_mode_entries)),
                    selectedValue = state.hudMode.toString(),
                    onValueChange = { viewModel.setHudMode(it.toInt()) },
                )
            }

            item {
                SectionTitle(stringResource(R.string.system))
                DropdownField(
                    label = stringResource(R.string.box64_preset),
                    options = state.box64Presets,
                    selectedValue = state.box64Preset,
                    onValueChange = viewModel::setBox64Preset,
                )
                Spacer(Modifier.height(12.dp))
                EnvironmentVariablesEditorHeader(state = state, viewModel = viewModel)
            }

            if (state.envVarItems.isEmpty()) {
                item(key = "env-empty") {
                    EmptyEditorText()
                }
            }
            else {
                itemsIndexed(
                    items = state.envVarItems,
                    key = { index, item -> "env-$index-${item.name}" },
                ) { index, item ->
                    EnvironmentVariableEditorItem(
                        item = item,
                        index = index,
                        viewModel = viewModel,
                    )
                }
            }

            item {
                SectionTitle(stringResource(R.string.advanced))
                DrivesEditorHeader(viewModel = viewModel)
            }

            if (state.driveItems.isEmpty()) {
                item(key = "drive-empty") {
                    EmptyEditorText()
                }
            }
            else {
                itemsIndexed(
                    items = state.driveItems,
                    key = { index, item -> "drive-$index-${item.letter}" },
                ) { index, item ->
                    DriveEditorItem(
                        item = item,
                        index = index,
                        locations = state.driveLocations,
                        viewModel = viewModel,
                        onBrowsePath = onBrowseDrivePath,
                    )
                }
            }

            val directXItems = state.winComponentItems.withIndex().filter { isDirectXWinComponent(it.value) }
            val generalItems = state.winComponentItems.withIndex().filterNot { isDirectXWinComponent(it.value) }

            item(key = "win-components-header") {
                WinComponentsEditorHeader()
                WinComponentGroupTitle(stringResource(R.string.directx))
            }
            items(
                items = directXItems,
                key = { "win-direct-${it.value.name}" },
            ) { indexedItem ->
                WinComponentEditorItem(
                    indexedItem = indexedItem,
                    onValueChange = viewModel::updateWinComponent,
                )
            }
            item(key = "win-components-general") {
                WinComponentGroupTitle(stringResource(R.string.general))
            }
            items(
                items = generalItems,
                key = { "win-general-${it.value.name}" },
            ) { indexedItem ->
                WinComponentEditorItem(
                    indexedItem = indexedItem,
                    onValueChange = viewModel::updateWinComponent,
                )
            }

            item {
                RawConfigurationEditor(state = state, viewModel = viewModel)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.ok))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
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
private fun DropdownField(
    label: String,
    options: List<DetailOption>,
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
private fun screenSizeOptions(): List<DetailOption> {
    val labels = stringArrayResource(R.array.screen_size_entries)
    return labels.mapIndexed { index, label ->
        val value = if (index == 0) {
            DetailViewModel.CUSTOM_SCREEN_SIZE
        }
        else {
            label.substringBefore(" ")
        }
        DetailOption(value, label)
    }
}

private fun indexedOptions(labels: Array<String>): List<DetailOption> {
    return labels.mapIndexed { index, label -> DetailOption(index.toString(), label) }
}
