@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.settings

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.R
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val application = LocalContext.current.applicationContext as Application
    val activity = LocalContext.current as? Activity
    val navController = LocalNavController.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(application) }
        },
    )
    val state by viewModel.state.collectAsState()

    SettingsContent(
        state = state,
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onSave = {
            val shouldRestart = viewModel.save()
            if (shouldRestart) {
                activity?.recreate()
            }
            else {
                navController.popBackStack()
            }
        },
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.ok))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionTitle(stringResource(R.string.mouse))
                SettingsSlider(
                    label = stringResource(R.string.cursor_speed),
                    value = state.cursorSpeed,
                    valueRange = 10f..200f,
                    onValueChange = viewModel::setCursorSpeed,
                )
                SettingsSlider(
                    label = stringResource(R.string.cursor_size),
                    value = state.cursorScale,
                    valueRange = 100f..200f,
                    onValueChange = viewModel::setCursorScale,
                )
                ColorPickerRow(
                    selected = state.cursorColor,
                    onSelected = viewModel::setCursorColor,
                )
                SettingsSwitch(
                    title = stringResource(R.string.move_cursor_to_touchpoint),
                    checked = state.moveCursorToTouchpoint,
                    onCheckedChange = viewModel::setMoveCursorToTouchpoint,
                )
                SettingsSwitch(
                    title = stringResource(R.string.capture_pointer_on_external_mouse),
                    checked = state.capturePointerOnExternalMouse,
                    onCheckedChange = viewModel::setCapturePointerOnExternalMouse,
                )
            }

            item {
                SettingsSectionTitle(stringResource(R.string.system))
                DropdownField(
                    label = stringResource(R.string.theme),
                    options = listOf(
                        SettingsOption(APP_THEME_LIGHT.toString(), stringResource(R.string.light)),
                        SettingsOption(APP_THEME_DARK.toString(), stringResource(R.string.dark)),
                    ),
                    selectedValue = state.appTheme.toString(),
                    onValueChange = { viewModel.setAppTheme(it.toInt()) },
                )
                Spacer(Modifier.height(12.dp))
                DropdownField(
                    label = stringResource(R.string.language),
                    options = indexedOptions(stringArrayResource(R.array.language_entries)),
                    selectedValue = state.languageIndex.toString(),
                    onValueChange = { viewModel.setLanguageIndex(it.toInt()) },
                )
                SettingsSwitch(
                    title = stringResource(R.string.open_android_browser_from_wine),
                    checked = state.openAndroidBrowserFromWine,
                    onCheckedChange = viewModel::setOpenAndroidBrowserFromWine,
                )
                SettingsSwitch(
                    title = stringResource(R.string.use_android_clipboard_on_Wine),
                    checked = state.useAndroidClipboardOnWine,
                    onCheckedChange = viewModel::setUseAndroidClipboardOnWine,
                )
            }

            item {
                SettingsSectionTitle(stringResource(R.string.box64))
                DropdownField(
                    label = stringResource(R.string.box64_preset),
                    options = state.box64Presets,
                    selectedValue = state.box64Preset,
                    onValueChange = viewModel::setBox64Preset,
                )
            }

            item {
                SettingsSectionTitle(stringResource(R.string.logs))
                SettingsSwitch(
                    title = stringResource(R.string.enable_wine_debug),
                    checked = state.enableWineDebug,
                    onCheckedChange = viewModel::setEnableWineDebug,
                )
                DropdownField(
                    label = stringResource(R.string.box64_logs),
                    options = indexedOptions(stringArrayResource(R.array.box64_logs)),
                    selectedValue = state.box64Logs.toString(),
                    onValueChange = { viewModel.setBox64Logs(it.toInt()) },
                )
                SettingsSwitch(
                    title = stringResource(R.string.save_logs_to_file),
                    checked = state.saveLogsToFile,
                    onCheckedChange = viewModel::setSaveLogsToFile,
                )
                if (state.saveLogsToFile) {
                    OutlinedTextField(
                        value = state.logFile,
                        onValueChange = viewModel::setLogFile,
                        label = { Text(stringResource(R.string.target_path)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                TextButton(
                    onClick = onSave,
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
private fun SettingsSectionTitle(text: String) {
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
        Text("${value.roundToInt()}%", style = MaterialTheme.typography.labelLarge)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = ((valueRange.endInclusive - valueRange.start) / 10f).roundToInt() - 1,
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
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ColorPickerRow(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    val colors = listOf(0xffffff, 0x000000, 0x651fff, 0xffea00, 0xff9100, 0xf50057, 0x00b0ff, 0x1de9b6)
    Text(
        text = stringResource(R.string.color),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { color ->
            val selectedColor = selected == color
            Box(
                modifier = Modifier
                    .size(if (selectedColor) 34.dp else 30.dp)
                    .clip(CircleShape)
                    .background(Color(color or 0xff000000.toInt()))
                    .clickable { onSelected(color) },
            )
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    options: List<SettingsOption>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label
        ?: options.firstOrNull()?.label
        ?: selectedValue

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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        onValueChange(option.value)
                    },
                )
            }
        }
    }
}

private fun indexedOptions(labels: Array<String>): List<SettingsOption> {
    return labels.mapIndexed { index, label -> SettingsOption(index.toString(), label) }
}
