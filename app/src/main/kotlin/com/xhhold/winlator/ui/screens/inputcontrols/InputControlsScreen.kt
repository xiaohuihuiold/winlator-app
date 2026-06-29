@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.inputcontrols

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.winlator.ControlsEditorActivity
import com.winlator.ExternalControllerBindingsActivity
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.R
import kotlin.math.roundToInt

@Composable
fun InputControlsScreen() {
    val application = LocalContext.current.applicationContext as Application
    val context = LocalContext.current
    val navController = LocalNavController.current
    val viewModel: InputControlsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { InputControlsViewModel(application) }
        },
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var prompt by remember { mutableStateOf<ProfilePrompt?>(null) }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    var pendingControllerRemoval by remember { mutableStateOf<ExternalControllerUiState?>(null) }
    val openProfileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importProfile(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    InputControlsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        importMenuExpanded = importMenuExpanded,
        onImportMenuExpandedChange = { importMenuExpanded = it },
        onBack = { navController.popBackStack() },
        onSelectProfile = { viewModel.selectProfile(it) },
        onAddProfile = { prompt = ProfilePrompt(ProfilePromptMode.Create, "") },
        onEditProfile = {
            val name = state.profiles.firstOrNull { it.id == state.selectedProfileId }?.name
            if (name != null) {
                prompt = ProfilePrompt(ProfilePromptMode.Rename, name)
            }
            else {
                viewModel.requireProfile()
            }
        },
        onDuplicateProfile = { confirmAction = ConfirmAction.DuplicateProfile },
        onRemoveProfile = { confirmAction = ConfirmAction.RemoveProfile },
        onCursorSpeedChange = viewModel::setCursorSpeed,
        onOverlayOpacityChange = viewModel::setOverlayOpacity,
        onDisableMouseInputChange = viewModel::setDisableMouseInput,
        onOpenProfileFile = {
            importMenuExpanded = false
            openProfileLauncher.launch(arrayOf("*/*"))
        },
        onDownloadProfile = {
            importMenuExpanded = false
            viewModel.downloadProfileList()
        },
        onExportProfile = viewModel::exportSelectedProfile,
        onOpenControlsEditor = {
            val profileId = state.selectedProfileId
            if (profileId != null) {
                context.startActivity(
                    Intent(context, ControlsEditorActivity::class.java).putExtra("profile_id", profileId),
                )
            }
            else {
                viewModel.requireProfile()
            }
        },
        onOpenController = { controller ->
            val profileId = state.selectedProfileId
            if (profileId != null) {
                context.startActivity(
                    Intent(context, ExternalControllerBindingsActivity::class.java)
                        .putExtra("profile_id", profileId)
                        .putExtra("controller_id", controller.id),
                )
            }
            else {
                viewModel.requireProfile()
            }
        },
        onRemoveController = { pendingControllerRemoval = it },
    )

    prompt?.let { request ->
        ProfileNameDialog(
            title = stringResource(R.string.profile_name),
            initialName = request.initialName,
            onDismiss = { prompt = null },
            onConfirm = { name ->
                prompt = null
                when (request.mode) {
                    ProfilePromptMode.Create -> viewModel.createProfile(name)
                    ProfilePromptMode.Rename -> viewModel.renameSelectedProfile(name)
                }
            },
        )
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(stringResource(if (action == ConfirmAction.RemoveProfile) R.string.remove else R.string.duplicate))
            },
            text = {
                Text(
                    stringResource(
                        if (action == ConfirmAction.RemoveProfile) {
                            R.string.do_you_want_to_remove_this_profile
                        }
                        else {
                            R.string.do_you_want_to_duplicate_this_profile
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction = null
                        when (action) {
                            ConfirmAction.DuplicateProfile -> viewModel.duplicateSelectedProfile()
                            ConfirmAction.RemoveProfile -> viewModel.removeSelectedProfile()
                        }
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingControllerRemoval?.let { controller ->
        AlertDialog(
            onDismissRequest = { pendingControllerRemoval = null },
            title = { Text(stringResource(R.string.remove)) },
            text = { Text(stringResource(R.string.do_you_want_to_remove_this_controller)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingControllerRemoval = null
                        viewModel.removeController(controller.id)
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingControllerRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    state.downloadItems?.let { items ->
        DownloadProfilesDialog(
            items = items,
            onDismiss = viewModel::dismissDownloadDialog,
            onConfirm = viewModel::downloadProfiles,
        )
    }
}

@Composable
private fun InputControlsContent(
    state: InputControlsUiState,
    snackbarHostState: SnackbarHostState,
    importMenuExpanded: Boolean,
    onImportMenuExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSelectProfile: (Int?) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onDuplicateProfile: () -> Unit,
    onRemoveProfile: () -> Unit,
    onCursorSpeedChange: (Float) -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    onDisableMouseInputChange: (Boolean) -> Unit,
    onOpenProfileFile: () -> Unit,
    onDownloadProfile: () -> Unit,
    onExportProfile: () -> Unit,
    onOpenControlsEditor: () -> Unit,
    onOpenController: (ExternalControllerUiState) -> Unit,
    onRemoveController: (ExternalControllerUiState) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.input_controls)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionTitle(stringResource(R.string.profile))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DropdownField(
                            label = stringResource(R.string.select_profile),
                            options = listOf(InputProfileOption(0, "-- ${stringResource(R.string.select_profile)} --")) + state.profiles,
                            selectedValue = state.selectedProfileId ?: 0,
                            onValueChange = { onSelectProfile(it.takeIf { id -> id > 0 }) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onAddProfile) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                        }
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = onDuplicateProfile) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.duplicate))
                        }
                        IconButton(onClick = onRemoveProfile) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.remove),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.mouse))
                    SettingsSlider(
                        label = stringResource(R.string.cursor_speed),
                        value = state.cursorSpeed,
                        valueRange = 10f..250f,
                        onValueChange = onCursorSpeedChange,
                    )
                    SettingsSwitch(
                        title = stringResource(R.string.disable_mouse_input),
                        checked = state.disableMouseInput,
                        onCheckedChange = onDisableMouseInputChange,
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.opacity))
                    SettingsSlider(
                        label = stringResource(R.string.overlay_opacity),
                        value = state.overlayOpacity,
                        valueRange = 10f..100f,
                        onValueChange = onOverlayOpacityChange,
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.import_profile))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { onImportMenuExpandedChange(true) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.import_profile))
                            }
                            DropdownMenu(
                                expanded = importMenuExpanded,
                                onDismissRequest = { onImportMenuExpandedChange(false) },
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                    text = { Text(stringResource(R.string.open_file)) },
                                    onClick = onOpenProfileFile,
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                    text = { Text(stringResource(R.string.download_file)) },
                                    onClick = onDownloadProfile,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onExportProfile,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.export_profile))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onOpenControlsEditor,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.controls_editor))
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.external_controllers))
                    if (state.controllers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_items_to_display),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }

                items(state.controllers, key = { it.id }) { controller ->
                    ControllerRow(
                        controller = controller,
                        onClick = { onOpenController(controller) },
                        onRemove = { onRemoveController(controller) },
                    )
                    HorizontalDivider()
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ControllerRow(
    controller: ExternalControllerUiState,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                Icons.Default.Gamepad,
                contentDescription = null,
                tint = if (controller.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        },
        headlineContent = {
            Text(
                text = controller.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text("${controller.bindingCount} ${stringResource(R.string.bindings).lowercase()}")
        },
        trailingContent = {
            if (controller.bindingCount > 0) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Composable
private fun ProfileNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
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
private fun DownloadProfilesDialog(
    items: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val selectedItems = remember(items) { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_profile)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                items(items, key = { it }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedItems.contains(item)) {
                                    selectedItems.remove(item)
                                }
                                else {
                                    selectedItems.add(item)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedItems.contains(item),
                            onCheckedChange = { checked ->
                                if (checked) selectedItems.add(item) else selectedItems.remove(item)
                            },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(item, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedItems.toList()) },
                enabled = selectedItems.isNotEmpty(),
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
private fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "${value.roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Slider(
        value = value.coerceIn(valueRange),
        onValueChange = onValueChange,
        valueRange = valueRange,
    )
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
    options: List<InputProfileOption>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedValue }?.name
        ?: options.firstOrNull()?.name
        ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onValueChange(option.id)
                    },
                )
            }
        }
    }
}

private data class ProfilePrompt(
    val mode: ProfilePromptMode,
    val initialName: String,
)

private enum class ProfilePromptMode {
    Create,
    Rename,
}

private enum class ConfirmAction {
    DuplicateProfile,
    RemoveProfile,
}
