package com.farazinc.webly.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farazinc.webly.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
    modifier: Modifier = Modifier
) {
    val isJavaScriptEnabled by viewModel.isJavaScriptEnabled.collectAsState(initial = true)
    val isAdBlockingEnabled by viewModel.isAdBlockingEnabled.collectAsState(initial = true)
    val areImagesEnabled by viewModel.areImagesEnabled.collectAsState(initial = true)
    val areCookiesEnabled by viewModel.areCookiesEnabled.collectAsState(initial = true)
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState(initial = true)
    val sendDoNotTrack by viewModel.sendDoNotTrack.collectAsState(initial = true)
    val isDesktopMode by viewModel.isDesktopMode.collectAsState(initial = false)
    val searchEngine by viewModel.searchEngine.collectAsState(initial = "https://www.google.com/search?q=")
    val homePage by viewModel.homePage.collectAsState(initial = "https://www.google.com")
    
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showHomePageDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "General") {
                SettingsSwitchItem(
                    title = "JavaScript",
                    description = "Enable JavaScript execution on websites",
                    checked = isJavaScriptEnabled,
                    onCheckedChange = { viewModel.setJavaScriptEnabled(it) }
                )
                
                SettingsSwitchItem(
                    title = "Load Images",
                    description = "Automatically load images on websites",
                    checked = areImagesEnabled,
                    onCheckedChange = { viewModel.setImagesEnabled(it) }
                )
                
                SettingsSwitchItem(
                    title = "Desktop Mode",
                    description = "Request desktop version of websites",
                    checked = isDesktopMode,
                    onCheckedChange = { viewModel.setDesktopMode(it) }
                )
            }
            
            HorizontalDivider()
            
            SettingsSection(title = "Privacy & Security") {
                SettingsSwitchItem(
                    title = "Ad Blocking",
                    description = "Block ads and trackers (80+ filters)",
                    checked = isAdBlockingEnabled,
                    onCheckedChange = { viewModel.setAdBlockingEnabled(it) },
                    highlight = true
                )
                
                SettingsSwitchItem(
                    title = "Enable Cookies",
                    description = "Allow websites to store cookies",
                    checked = areCookiesEnabled,
                    onCheckedChange = { viewModel.setCookiesEnabled(it) }
                )
                
                SettingsSwitchItem(
                    title = "Block Third-Party Cookies",
                    description = "Block cookies from advertisers and trackers",
                    checked = blockThirdPartyCookies,
                    onCheckedChange = { viewModel.setBlockThirdPartyCookies(it) },
                    enabled = areCookiesEnabled
                )
                
                SettingsSwitchItem(
                    title = "Do Not Track",
                    description = "Send Do Not Track header with requests",
                    checked = sendDoNotTrack,
                    onCheckedChange = { viewModel.setSendDoNotTrack(it) }
                )
            }
            
            HorizontalDivider()
            
            SettingsSection(title = "Search & Homepage") {
                SettingsClickableItem(
                    title = "Search Engine",
                    description = getSearchEngineName(searchEngine),
                    onClick = { showSearchEngineDialog = true }
                )
                
                SettingsClickableItem(
                    title = "Homepage",
                    description = homePage,
                    onClick = { showHomePageDialog = true }
                )
            }
            
            HorizontalDivider()
            
            SettingsSection(title = "Data Management") {
                SettingsClickableItem(
                    title = "Clear Browsing Data",
                    description = "Clear cookies, cache, and history",
                    onClick = { showClearDataDialog = true },
                    destructive = true
                )
            }
            
            HorizontalDivider()
            
            SettingsSection(title = "About") {
                SettingsInfoItem(
                    title = "Version",
                    value = "1.0.0"
                )
                
                SettingsInfoItem(
                    title = "Build",
                    value = "Debug"
                )
                
                SettingsInfoItem(
                    title = "Filter Rules",
                    value = "80+"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Webly - Lightweight Browser with Ad-Blocking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    
    if (showSearchEngineDialog) {
        SearchEngineDialog(
            currentEngine = searchEngine,
            onDismiss = { showSearchEngineDialog = false },
            onSelect = { engine ->
                viewModel.setSearchEngine(engine)
                showSearchEngineDialog = false
            }
        )
    }
    
    if (showHomePageDialog) {
        HomePageDialog(
            currentHomePage = homePage,
            onDismiss = { showHomePageDialog = false },
            onSave = { page ->
                viewModel.setHomePage(page)
                showHomePageDialog = false
            }
        )
    }
    
    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = { clearCookies, clearCache, clearHistory ->
                viewModel.clearBrowsingData(clearCookies, clearCache, clearHistory)
                showClearDataDialog = false
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
        content()
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
               else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (destructive) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SearchEngineDialog(
    currentEngine: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val engines = listOf(
        "Google" to "https://www.google.com/search?q=",
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Bing" to "https://www.bing.com/search?q=",
        "Yahoo" to "https://search.yahoo.com/search?p="
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Search Engine") },
        text = {
            Column {
                engines.forEach { (name, url) ->
                    val isSelected = currentEngine == url
                    Surface(
                        onClick = { onSelect(url) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HomePageDialog(
    currentHomePage: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentHomePage) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Homepage") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Homepage URL") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
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
fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (clearCookies: Boolean, clearCache: Boolean, clearHistory: Boolean) -> Unit
) {
    var clearCookies by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearHistory by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Browsing Data") },
        text = {
            Column {
                Text(
                    text = "Select data to clear:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearCookies,
                        onCheckedChange = { clearCookies = it }
                    )
                    Text("Cookies")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearCache,
                        onCheckedChange = { clearCache = it }
                    )
                    Text("Cache")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearHistory,
                        onCheckedChange = { clearHistory = it }
                    )
                    Text("History")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(clearCookies, clearCache, clearHistory) }
            ) {
                Text("Clear", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getSearchEngineName(url: String): String {
    return when {
        url.contains("google.com") -> "Google"
        url.contains("duckduckgo.com") -> "DuckDuckGo"
        url.contains("bing.com") -> "Bing"
        url.contains("yahoo.com") -> "Yahoo"
        else -> "Custom"
    }
}
