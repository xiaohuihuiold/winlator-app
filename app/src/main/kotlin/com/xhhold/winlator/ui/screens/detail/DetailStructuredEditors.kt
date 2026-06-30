@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.xhhold.winlator.ui.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xhhold.winlator.R

@Composable
fun EnvironmentVariablesEditor(
    state: DetailUiState,
    viewModel: DetailViewModel,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    EditorHeader(
        title = stringResource(R.string.environment_variables),
        onAddClick = { showAddDialog = true },
    )

    if (state.envVarItems.isEmpty()) {
        EmptyEditorText()
    }
    else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.envVarItems.forEachIndexed { index, item ->
                EnvVarRow(
                    item = item,
                    index = index,
                    onNameChange = viewModel::updateEnvVarName,
                    onValueChange = viewModel::updateEnvVarValue,
                    onRemove = viewModel::removeEnvVar,
                )
            }
        }
    }

    if (showAddDialog) {
        AddEnvVarDialog(
            existingNames = state.envVarItems.map { it.name }.toSet(),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, value ->
                showAddDialog = false
                viewModel.addEnvVar(name, value)
            },
        )
    }
}

@Composable
fun EnvironmentVariablesEditorHeader(
    state: DetailUiState,
    viewModel: DetailViewModel,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    EditorHeader(
        title = stringResource(R.string.environment_variables),
        onAddClick = { showAddDialog = true },
    )

    if (showAddDialog) {
        AddEnvVarDialog(
            existingNames = state.envVarItems.map { it.name }.toSet(),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, value ->
                showAddDialog = false
                viewModel.addEnvVar(name, value)
            },
        )
    }
}

@Composable
fun EnvironmentVariableEditorItem(
    item: DetailEnvVarItem,
    index: Int,
    viewModel: DetailViewModel,
) {
    EnvVarRow(
        item = item,
        index = index,
        onNameChange = viewModel::updateEnvVarName,
        onValueChange = viewModel::updateEnvVarValue,
        onRemove = viewModel::removeEnvVar,
    )
}

@Composable
fun DrivesEditor(
    state: DetailUiState,
    viewModel: DetailViewModel,
    onBrowsePath: (Int) -> Unit = {},
) {
    EditorHeader(
        title = stringResource(R.string.drives),
        onAddClick = viewModel::addDrive,
    )

    if (state.driveItems.isEmpty()) {
        EmptyEditorText()
    }
    else {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            state.driveItems.forEachIndexed { index, item ->
                DriveRow(
                    item = item,
                    index = index,
                    locations = state.driveLocations,
                    onLetterChange = viewModel::updateDriveLetter,
                    onPathChange = viewModel::updateDrivePath,
                    onBrowsePath = onBrowsePath,
                    onRemove = viewModel::removeDrive,
                )
            }
        }
    }
}

@Composable
fun DrivesEditorHeader(
    viewModel: DetailViewModel,
) {
    EditorHeader(
        title = stringResource(R.string.drives),
        onAddClick = viewModel::addDrive,
    )
}

@Composable
fun DriveEditorItem(
    item: DetailDriveItem,
    index: Int,
    locations: List<DetailOption>,
    viewModel: DetailViewModel,
    onBrowsePath: (Int) -> Unit,
) {
    DriveRow(
        item = item,
        index = index,
        locations = locations,
        onLetterChange = viewModel::updateDriveLetter,
        onPathChange = viewModel::updateDrivePath,
        onBrowsePath = onBrowsePath,
        onRemove = viewModel::removeDrive,
    )
}

@Composable
fun WinComponentsEditor(
    state: DetailUiState,
    viewModel: DetailViewModel,
) {
    EditorHeader(title = stringResource(R.string.win_components))

    val directXItems = state.winComponentItems.withIndex()
        .filter { isDirectXWinComponent(it.value) }
    val generalItems = state.winComponentItems.withIndex()
        .filterNot { isDirectXWinComponent(it.value) }

    WinComponentGroup(
        title = stringResource(R.string.directx),
        items = directXItems,
        onValueChange = viewModel::updateWinComponent,
    )
    Spacer(Modifier.height(12.dp))
    WinComponentGroup(
        title = stringResource(R.string.general),
        items = generalItems,
        onValueChange = viewModel::updateWinComponent,
    )
}

@Composable
fun WinComponentsEditorHeader() {
    EditorHeader(title = stringResource(R.string.win_components))
}

fun isDirectXWinComponent(item: DetailWinComponentItem): Boolean {
    return item.name.startsWith("direct") || item.name.startsWith("x")
}

@Composable
fun WinComponentGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun WinComponentEditorItem(
    indexedItem: IndexedValue<DetailWinComponentItem>,
    onValueChange: (Int, Int) -> Unit,
) {
    DropdownField(
        label = winComponentLabel(indexedItem.value.name),
        options = listOf(
            DetailOption("0", stringResource(R.string.builtin_wine)),
            DetailOption("1", stringResource(R.string.native_windows)),
        ),
        selectedValue = indexedItem.value.value.coerceIn(0, 1).toString(),
        onValueChange = { onValueChange(indexedItem.index, it.toInt()) },
    )
}

@Composable
fun RawConfigurationEditor(
    state: DetailUiState,
    viewModel: DetailViewModel,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text("${stringResource(R.string.advanced)} ${stringResource(R.string.configuration)}")
    }

    if (expanded) {
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.envVars,
                onValueChange = viewModel::setEnvVars,
                label = { Text(stringResource(R.string.environment_variables)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.drives,
                onValueChange = viewModel::setDrives,
                label = { Text(stringResource(R.string.drives)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.winComponents,
                onValueChange = viewModel::setWinComponents,
                label = { Text(stringResource(R.string.win_components)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EnvVarRow(
    item: DetailEnvVarItem,
    index: Int,
    onNameChange: (Int, String) -> Unit,
    onValueChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val known = DetailViewModel.knownEnvVar(item.name)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (known == null) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onNameChange(index, it) },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = known.type.name.lowercase().replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onRemove(index) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        when (known?.type) {
            DetailEnvVarType.CHECKBOX -> EnvVarSwitch(
                item = item,
                known = known,
                index = index,
                onValueChange = onValueChange,
            )

            DetailEnvVarType.SELECT -> DropdownField(
                label = stringResource(R.string.value),
                options = known.options.map { DetailOption(it, it) },
                selectedValue = item.value,
                onValueChange = { onValueChange(index, it) },
            )

            DetailEnvVarType.SELECT_MULTIPLE -> MultiSelectEnvVar(
                item = item,
                known = known,
                index = index,
                onValueChange = onValueChange,
            )

            DetailEnvVarType.NUMBER -> OutlinedTextField(
                value = item.value,
                onValueChange = { onValueChange(index, it) },
                label = { Text(stringResource(R.string.value)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            DetailEnvVarType.TEXT,
            null -> OutlinedTextField(
                value = item.value,
                onValueChange = { onValueChange(index, it) },
                label = { Text(stringResource(R.string.value)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun EnvVarSwitch(
    item: DetailEnvVarItem,
    known: DetailKnownEnvVar,
    index: Int,
    onValueChange: (Int, String) -> Unit,
) {
    val offValue = known.options.getOrElse(0) { "0" }
    val onValue = known.options.getOrElse(1) { "1" }
    val checked = item.value == onValue || item.value == "1" || item.value == "true"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onValueChange(index, if (checked) offValue else onValue) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${stringResource(R.string.value)}: ${if (checked) onValue else offValue}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = { onValueChange(index, if (it) onValue else offValue) },
        )
    }
}

@Composable
private fun MultiSelectEnvVar(
    item: DetailEnvVarItem,
    known: DetailKnownEnvVar,
    index: Int,
    onValueChange: (Int, String) -> Unit,
) {
    val selected = item.value.split(",").filter { it.isNotBlank() }.toSet()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        known.options.forEach { option ->
            val isSelected = option in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    val next = if (isSelected) selected - option else selected + option
                    onValueChange(index, next.joinToString(","))
                },
                label = { Text(option) },
            )
        }
    }
}

@Composable
private fun DriveRow(
    item: DetailDriveItem,
    index: Int,
    locations: List<DetailOption>,
    onLetterChange: (Int, String) -> Unit,
    onPathChange: (Int, String) -> Unit,
    onBrowsePath: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DropdownField(
                label = stringResource(R.string.letter),
                options = DetailViewModel.DRIVE_LETTERS.map { DetailOption(it, "$it:") },
                selectedValue = item.letter,
                onValueChange = { onLetterChange(index, it) },
                modifier = Modifier.width(104.dp),
            )
            OutlinedButton(
                onClick = { onBrowsePath(index) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.open_directory),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.open_directory),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onRemove(index) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        OutlinedTextField(
            value = item.path,
            onValueChange = { onPathChange(index, it) },
            label = { Text(stringResource(R.string.target_path)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownField(
            label = stringResource(R.string.locations),
            options = listOf(DetailOption("", stringResource(R.string.locations))) + locations,
            selectedValue = "",
            onValueChange = { if (it.isNotEmpty()) onPathChange(index, it) },
        )
        HorizontalDivider()
    }
}

@Composable
private fun WinComponentGroup(
    title: String,
    items: List<IndexedValue<DetailWinComponentItem>>,
    onValueChange: (Int, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        items.forEach { indexedItem ->
            DropdownField(
                label = winComponentLabel(indexedItem.value.name),
                options = listOf(
                    DetailOption("0", stringResource(R.string.builtin_wine)),
                    DetailOption("1", stringResource(R.string.native_windows)),
                ),
                selectedValue = indexedItem.value.value.coerceIn(0, 1).toString(),
                onValueChange = { onValueChange(indexedItem.index, it.toInt()) },
            )
        }
    }
}

@Composable
private fun AddEnvVarDialog(
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    val knownOptions = DetailViewModel.KNOWN_ENV_VARS
        .filterNot { it.name in existingNames }
        .map { DetailOption(it.name, it.name) }
    var selectedKnownName by remember(knownOptions) { mutableStateOf(knownOptions.firstOrNull()?.value.orEmpty()) }
    var customName by remember { mutableStateOf("") }
    var customValue by remember { mutableStateOf("") }
    var useCustomName by remember(knownOptions) { mutableStateOf(knownOptions.isEmpty()) }
    val targetName = if (useCustomName) customName.trim() else selectedKnownName.trim()
    val targetValue = if (useCustomName) customValue else null
    val isDuplicate = targetName in existingNames

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_environment_variable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !useCustomName,
                        onClick = { useCustomName = false },
                        enabled = knownOptions.isNotEmpty(),
                        label = { Text(stringResource(R.string.known_environment_variable)) },
                    )
                    FilterChip(
                        selected = useCustomName,
                        onClick = { useCustomName = true },
                        label = { Text(stringResource(R.string.custom_environment_variable)) },
                    )
                }
                if (!useCustomName && knownOptions.isNotEmpty()) {
                    DropdownField(
                        label = stringResource(R.string.environment_variables),
                        options = knownOptions,
                        selectedValue = selectedKnownName,
                        onValueChange = { selectedKnownName = it },
                    )
                }
                if (useCustomName) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it.trim().replace(" ", "") },
                        label = { Text("KEY") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { customValue = it.trim().replace(" ", "") },
                        label = { Text(stringResource(R.string.value)) },
                        placeholder = { Text(stringResource(R.string.custom_environment_value_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (isDuplicate) {
                    Text(
                        text = stringResource(R.string.environment_variable_already_exists),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(targetName, targetValue) },
                enabled = targetName.isNotBlank() && !isDuplicate,
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
private fun EditorHeader(
    title: String,
    onAddClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onAddClick != null) {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    }
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))
}

@Composable
fun EmptyEditorText() {
    Text(
        text = stringResource(R.string.no_items_to_display),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
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
private fun winComponentLabel(name: String): String {
    return when (name) {
        "direct3d" -> stringResource(R.string.direct3d)
        "directsound" -> stringResource(R.string.directsound)
        "directmusic" -> stringResource(R.string.directmusic)
        "directshow" -> stringResource(R.string.directshow)
        "directplay" -> stringResource(R.string.directplay)
        "xaudio" -> stringResource(R.string.xaudio)
        "vcrun2005" -> stringResource(R.string.vcrun2005)
        "vcrun2010" -> stringResource(R.string.vcrun2010)
        "wmdecoder" -> stringResource(R.string.wmdecoder)
        else -> name
    }
}
