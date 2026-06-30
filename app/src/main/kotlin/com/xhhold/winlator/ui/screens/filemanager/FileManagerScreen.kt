@file:OptIn(ExperimentalMaterial3Api::class)

package com.xhhold.winlator.ui.screens.filemanager

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.winlator.XServerDisplayActivity
import com.winlator.container.FileInfo
import com.winlator.core.FileUtils
import com.winlator.core.StringUtils
import com.xhhold.winlator.LocalNavController
import com.xhhold.winlator.R

@Composable
fun FileManagerScreen(containerId: Int?) {
    val application = LocalContext.current.applicationContext as Application
    val context = LocalContext.current
    val navController = LocalNavController.current
    val resolvedContainerId = containerId ?: 0
    val viewModel: FileManagerViewModel = viewModel(
        key = "file-manager-$resolvedContainerId",
        factory = viewModelFactory {
            initializer { FileManagerViewModel(application, resolvedContainerId) }
        },
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateFolder by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<FileInfo?>(null) }
    var pendingRename by remember { mutableStateOf<FileInfo?>(null) }

    fun navigateBack() {
        if (!viewModel.goBack()) {
            navController.popBackStack()
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

    FileManagerContent(
        state = state,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onBack = ::navigateBack,
        onHome = viewModel::goHome,
        onCreateFolder = { showCreateFolder = true },
        onPaste = viewModel::pasteFiles,
        onToggleStyle = {
            val nextStyle = if (state.viewStyle == FileManagerViewStyle.GRID) FileManagerViewStyle.LIST else FileManagerViewStyle.GRID
            viewModel.setViewStyle(nextStyle)
        },
        onFileClick = { file ->
            if (viewModel.isRunnableFile(file)) {
                context.startActivity(
                    Intent(context, XServerDisplayActivity::class.java).apply {
                        putExtra("container_id", viewModel.currentContainerId())
                        putExtra("exec_path", file.path)
                    },
                )
            }
            else {
                viewModel.openFolder(file)
            }
        },
        onAction = { file, action ->
            when (action) {
                FileAction.Copy -> viewModel.copyFile(file, cutMode = false)
                FileAction.Cut -> viewModel.copyFile(file, cutMode = true)
                FileAction.Remove -> pendingRemoval = file
                FileAction.Rename -> pendingRename = file
                FileAction.AddFavorite -> viewModel.addFavorite(file)
                FileAction.Info -> viewModel.showInfo(file)
            }
        },
    )

    if (showCreateFolder) {
        TextPromptDialog(
            title = stringResource(R.string.new_folder),
            initialValue = "",
            onDismiss = { showCreateFolder = false },
            onConfirm = { name ->
                showCreateFolder = false
                viewModel.createFolder(name)
            },
        )
    }

    pendingRename?.let { file ->
        TextPromptDialog(
            title = stringResource(R.string.rename),
            initialValue = file.name,
            onDismiss = { pendingRename = null },
            onConfirm = { name ->
                pendingRename = null
                viewModel.renameFile(file, name)
            },
        )
    }

    pendingRemoval?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove)) },
            text = { Text(stringResource(R.string.do_you_want_to_remove_this_file)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        viewModel.removeFile(file)
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

    state.details?.let { details ->
        FileDetailsDialog(details = details, onDismiss = viewModel::dismissInfo)
    }
}

@Composable
private fun FileManagerContent(
    state: FileManagerUiState,
    viewModel: FileManagerViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onCreateFolder: () -> Unit,
    onPaste: () -> Unit,
    onToggleStyle: () -> Unit,
    onFileClick: (FileInfo) -> Unit,
    onAction: (FileInfo, FileAction) -> Unit,
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
                    IconButton(onClick = onHome) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                    }
                    IconButton(onClick = onCreateFolder, enabled = state.canGoBack) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(R.string.new_folder))
                    }
                    IconButton(onClick = onPaste, enabled = state.canPaste) {
                        Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                    IconButton(onClick = onToggleStyle) {
                        val icon = if (state.viewStyle == FileManagerViewStyle.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView
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
            if (!state.exists) {
                EmptyFileState(
                    title = stringResource(R.string.empty_location_title),
                    body = stringResource(R.string.empty_location_body),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else if (state.files.isEmpty()) {
                EmptyFileState(
                    title = stringResource(R.string.empty_folder_title),
                    body = stringResource(R.string.empty_folder_body),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else if (state.viewStyle == FileManagerViewStyle.GRID) {
                FileGrid(
                    files = state.files,
                    viewModel = viewModel,
                    onFileClick = onFileClick,
                    onAction = onAction,
                )
            }
            else {
                FileList(
                    files = state.files,
                    viewModel = viewModel,
                    onFileClick = onFileClick,
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
private fun EmptyFileState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileGrid(
    files: List<FileInfo>,
    viewModel: FileManagerViewModel,
    onFileClick: (FileInfo) -> Unit,
    onAction: (FileInfo, FileAction) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        items(files, key = { it.path }) { file ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onFileClick(file) },
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FileIcon(
                        file = file,
                        viewModel = viewModel,
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
                                text = displayTitle(file, viewModel),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitle(file, viewModel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FileActions(file = file, isRoot = !viewModel.state.value.canGoBack, onAction = onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileList(
    files: List<FileInfo>,
    viewModel: FileManagerViewModel,
    onFileClick: (FileInfo) -> Unit,
    onAction: (FileInfo, FileAction) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files, key = { it.path }) { file ->
            ListItem(
                modifier = Modifier.clickable { onFileClick(file) },
                leadingContent = {
                    FileIcon(file = file, viewModel = viewModel, modifier = Modifier.size(42.dp))
                },
                headlineContent = {
                    Text(
                        text = displayTitle(file, viewModel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = subtitle(file, viewModel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isRunnable = viewModel.isRunnableFile(file)
                        IconButton(onClick = { onFileClick(file) }) {
                            Icon(
                                if (isRunnable) Icons.Default.PlayArrow else Icons.Default.FolderOpen,
                                contentDescription = stringResource(if (isRunnable) R.string.run else R.string.open_directory),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        FileActions(file = file, isRoot = !viewModel.state.value.canGoBack, onAction = onAction)
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FileIcon(
    file: FileInfo,
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(fileIconRes(file, viewModel)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

private enum class FileAction {
    Copy,
    Cut,
    Remove,
    Rename,
    AddFavorite,
    Info,
}

@Composable
private fun FileActions(
    file: FileInfo,
    isRoot: Boolean,
    onAction: (FileInfo, FileAction) -> Unit,
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
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                text = { Text(stringResource(R.string.copy)) },
                onClick = {
                    expanded = false
                    onAction(file, FileAction.Copy)
                },
            )
            if (!isRoot) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                    text = { Text(stringResource(R.string.cut)) },
                    onClick = {
                        expanded = false
                        onAction(file, FileAction.Cut)
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                    text = { Text(stringResource(R.string.rename)) },
                    onClick = {
                        expanded = false
                        onAction(file, FileAction.Rename)
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_to_favorites)) },
                    onClick = {
                        expanded = false
                        onAction(file, FileAction.AddFavorite)
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
                        onAction(file, FileAction.Remove)
                    },
                )
            }
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                text = { Text(stringResource(R.string.information)) },
                onClick = {
                    expanded = false
                    onAction(file, FileAction.Info)
                },
            )
        }
    }
}

@Composable
private fun TextPromptDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
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
private fun FileDetailsDialog(
    details: FileInfoDetails,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.information)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(label = stringResource(R.string.name), value = details.name)
                DetailRow(label = stringResource(R.string.type), value = stringResource(details.type))
                DetailRow(label = stringResource(R.string.location), value = details.location)
                DetailRow(label = stringResource(R.string.modified), value = details.modified)
                if (details.type == R.string.folder) {
                    DetailRow(label = stringResource(R.string.contains), value = "${details.itemCount} ${stringResource(R.string.items)}")
                }
                else {
                    DetailRow(label = stringResource(R.string.size), value = StringUtils.formatBytes(details.size))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun subtitle(
    file: FileInfo,
    viewModel: FileManagerViewModel,
): String {
    return when (viewModel.effectiveType(file)) {
        FileInfo.Type.FILE -> StringUtils.formatBytes(file.size)
        FileInfo.Type.DIRECTORY -> "${file.itemCount} ${stringResource(R.string.items)}"
        FileInfo.Type.DRIVE -> ""
    }
}

@Composable
private fun displayTitle(
    file: FileInfo,
    viewModel: FileManagerViewModel,
): String {
    return if (file.type == FileInfo.Type.DRIVE) {
        "${stringResource(R.string.drive)} (${file.name})"
    }
    else {
        file.displayName
    }
}

private fun fileIconRes(
    file: FileInfo,
    viewModel: FileManagerViewModel,
): Int {
    return when (viewModel.effectiveType(file)) {
        FileInfo.Type.DRIVE -> R.drawable.container_drive
        FileInfo.Type.DIRECTORY -> R.drawable.container_folder
        FileInfo.Type.FILE -> when (FileUtils.getExtension(file.path)) {
            "exe", "bat" -> R.drawable.container_file_window
            "dll" -> R.drawable.container_file_library
            "lnk" -> R.drawable.container_file_link
            else -> R.drawable.container_file
        }
    }
}
