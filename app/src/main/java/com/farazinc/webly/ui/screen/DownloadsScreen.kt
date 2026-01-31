package com.farazinc.webly.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farazinc.webly.data.model.DownloadItem
import com.farazinc.webly.domain.DownloadManagerWrapper
import com.farazinc.webly.ui.viewmodel.DownloadsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
    modifier: Modifier = Modifier
) {
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val completedDownloads by viewModel.completedDownloads.collectAsState()
    val context = LocalContext.current
    
    var selectedDownload by remember { mutableStateOf<DownloadItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                title = { 
                    Column {
                        Text("Downloads")
                        val total = activeDownloads.size + completedDownloads.size
                        if (total > 0) {
                            Text(
                                text = "$total download${if (total != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (completedDownloads.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear completed",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text("Active (${activeDownloads.size})") 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text("Completed (${completedDownloads.size})") 
                    }
                )
            }
            
            when (selectedTab) {
                0 -> {
                    if (activeDownloads.isEmpty()) {
                        EmptyDownloadsState(
                            message = "No active downloads",
                            description = "Downloads in progress will appear here"
                        )
                    } else {
                        DownloadsList(
                            downloads = activeDownloads,
                            onOpenClick = { download ->
                                download.filePath?.let { path ->
                                    val uri = Uri.parse(path)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, download.mimeType)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                }
                            },
                            onDeleteClick = { download ->
                                selectedDownload = download
                                showDeleteDialog = true
                            },
                            showProgress = true
                        )
                    }
                }
                1 -> {
                    if (completedDownloads.isEmpty()) {
                        EmptyDownloadsState(
                            message = "No completed downloads",
                            description = "Completed downloads will appear here"
                        )
                    } else {
                        DownloadsList(
                            downloads = completedDownloads,
                            onOpenClick = { download ->
                                download.filePath?.let { path ->
                                    val uri = Uri.parse(path)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, download.mimeType)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Open with"))
                                    } catch (e: Exception) {
                                    }
                                }
                            },
                            onDeleteClick = { download ->
                                selectedDownload = download
                                showDeleteDialog = true
                            },
                            showProgress = false
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog && selectedDownload != null) {
        DeleteDownloadDialog(
            download = selectedDownload!!,
            onDismiss = { 
                showDeleteDialog = false
                selectedDownload = null
            },
            onConfirm = {
                selectedDownload?.let { viewModel.deleteDownload(it.id) }
                showDeleteDialog = false
                selectedDownload = null
            }
        )
    }
    
    if (showClearAllDialog) {
        ClearAllDownloadsDialog(
            onDismiss = { showClearAllDialog = false },
            onConfirm = {
                viewModel.clearCompletedDownloads()
                showClearAllDialog = false
            }
        )
    }
}

@Composable
fun DownloadsList(
    downloads: List<DownloadItem>,
    onOpenClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(downloads, key = { it.id }) { download ->
            DownloadItemCard(
                download = download,
                onOpenClick = { onOpenClick(download) },
                onDeleteClick = { onDeleteClick(download) },
                showProgress = showProgress
            )
        }
    }
}

@Composable
fun DownloadItemCard(
    download: DownloadItem,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = if (download.isComplete) { onOpenClick } else { {} }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = DownloadManagerWrapper.getMimeTypeIcon(download.mimeType),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            download.isComplete -> {
                                Text(
                                    text = "✓ Complete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = DownloadManagerWrapper.formatFileSize(download.totalBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            download.isFailed -> {
                                Text(
                                    text = "✗ Failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            download.isPaused -> {
                                Text(
                                    text = "❚❚ Paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            download.isRunning -> {
                                Text(
                                    text = "${download.progress}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${DownloadManagerWrapper.formatFileSize(download.downloadedBytes)} / ${DownloadManagerWrapper.formatFileSize(download.totalBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = formatDownloadTime(download.startTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (download.isComplete) {
                            DropdownMenuItem(
                                text = { Text("Open") },
                                onClick = {
                                    showMenu = false
                                    onOpenClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
            
            if (showProgress && download.isRunning) {
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyDownloadsState(
    message: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DeleteDownloadDialog(
    download: DownloadItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Download?") },
        text = {
            Column {
                Text("Remove this download from the list?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = download.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClearAllDownloadsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Clear Completed Downloads?") },
        text = {
            Text("This will remove all completed downloads from the list. Downloaded files will remain on your device.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDownloadTime(timestamp: Long): String {
    val format = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}
