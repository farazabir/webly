package com.farazinc.webly.ui.screen

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farazinc.webly.ui.components.BrowserSearchBar
import com.farazinc.webly.ui.components.BrowserBottomBar
import com.farazinc.webly.ui.components.BrowserWebView
import com.farazinc.webly.ui.viewmodel.BrowserViewModel

@Composable
fun BrowserApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var readerModeUrl by remember { mutableStateOf("") }
    var readerModeHtml by remember { mutableStateOf("") }
    var browserViewModel: BrowserViewModel? by remember { mutableStateOf(null) }
    
    NavHost(
        navController = navController,
        startDestination = "browser",
        modifier = modifier
    ) {
        composable("browser") {
            BrowserScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToBookmarks = { navController.navigate("bookmarks") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToDownloads = { navController.navigate("downloads") },
                onNavigateToReaderMode = { url, html ->
                    readerModeUrl = url
                    readerModeHtml = html
                    navController.navigate("reader")
                },
                onNavigateToSearch = { navController.navigate("search") },
                onViewModelCreated = { browserViewModel = it }
            )
        }
        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onSearch = { query ->
                    browserViewModel?.loadUrl(query)
                    navController.popBackStack()
                },
                onHistoryItemClick = { url ->
                    browserViewModel?.loadUrl(url)
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                onNavigateBack = { navController.popBackStack() },
                onBookmarkClick = { url ->
                    navController.popBackStack()
                }
            )
        }
        composable("history") {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onHistoryClick = { url ->
                    navController.popBackStack()
                }
            )
        }
        composable("downloads") {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("reader") {
            ReaderModeScreen(
                url = readerModeUrl,
                html = readerModeHtml,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToReaderMode: (String, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onViewModelCreated: (BrowserViewModel) -> Unit = {},
    viewModel: BrowserViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(viewModel) {
        onViewModelCreated(viewModel)
    }
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val showTabSwitcher by viewModel.showTabSwitcher.collectAsState()
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val currentHtml by viewModel.currentHtml.collectAsState()
    
    var webView: WebView? by remember { mutableStateOf(null) }
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrowserSearchBar(
                currentUrl = currentUrl,
                isLoading = isLoading,
                progress = progress,
                isBookmarked = isBookmarked,
                onUrlSubmit = { url ->
                    viewModel.loadUrl(url)
                },
                onStopClick = {
                    webView?.stopLoading()
                },
                onRefreshClick = {
                    webView?.reload()
                },
                onBookmarkClick = {
                    viewModel.toggleBookmark()
                },
                onReaderModeClick = {
                    webView?.evaluateJavascript("document.documentElement.outerHTML") { html ->
                        val cleanHtml = html?.removeSurrounding("\"")?.replace("\\\"", "\"")?.replace("\\n", "\n")
                        if (!cleanHtml.isNullOrEmpty() && currentUrl.isNotEmpty()) {
                            onNavigateToReaderMode(currentUrl, cleanHtml)
                        }
                    }
                },
                onSearchBarClick = {
                    onNavigateToSearch()
                }
            )
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                tabCount = tabs.size,
                onBackClick = {
                    webView?.goBack()
                },
                onForwardClick = {
                    webView?.goForward()
                },
                onHomeClick = {
                    viewModel.goHome()
                },
                onRefreshClick = {
                    webView?.reload()
                },
                onTabsClick = {
                    viewModel.toggleTabSwitcher()
                },
                onMenuClick = {
                    showMenu = true
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            BrowserWebView(
                url = currentUrl,
                adBlockEngine = viewModel.adBlockEngine,
                onPageStarted = { url, favicon ->
                    viewModel.onPageStarted(url, favicon)
                },
                onPageFinished = { url, title ->
                    viewModel.onPageFinished(url, title)
                },
                onProgressChanged = { prog ->
                    viewModel.onProgressChanged(prog)
                },
                onNavigationStateChanged = { back, forward ->
                    viewModel.updateNavigationState(back, forward)
                },
                modifier = Modifier.fillMaxSize(),
                webViewRef = { webView = it }
            )
            
            if (showTabSwitcher) {
                TabSwitcher(
                    tabs = tabs,
                    onTabClick = { tabId ->
                        viewModel.switchToTab(tabId)
                    },
                    onCloseTab = { tabId ->
                        viewModel.closeTab(tabId)
                    },
                    onNewTab = {
                        viewModel.createNewTab()
                        viewModel.toggleTabSwitcher()
                    },
                    onDismiss = {
                        viewModel.toggleTabSwitcher()
                    }
                )
            }
            
            if (showMenu) {
                BrowserMenu(
                    onDismiss = { showMenu = false },
                    onBookmarksClick = {
                        showMenu = false
                        onNavigateToBookmarks()
                    },
                    onHistoryClick = {
                        showMenu = false
                        onNavigateToHistory()
                    },
                    onDownloadsClick = {
                        showMenu = false
                        onNavigateToDownloads()
                    },
                    onSettingsClick = {
                        showMenu = false
                        onNavigateToSettings()
                    }
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (currentUrl.isEmpty()) {
            viewModel.goHome()
        }
    }
}

@Composable
fun TabSwitcher(
    tabs: List<com.farazinc.webly.data.model.Tab>,
    onTabClick: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tabs (${tabs.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onNewTab) {
                    Text("New Tab")
                }
            }
            
            tabs.forEach { tab ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = { onTabClick(tab.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title.ifEmpty { "New Tab" },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = tab.url.ifEmpty { "about:blank" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { onCloseTab(tab.id) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserMenu(
    onDismiss: () -> Unit,
    onBookmarksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Bookmarks") },
            onClick = onBookmarksClick
        )
        DropdownMenuItem(
            text = { Text("History") },
            onClick = onHistoryClick
        )
        DropdownMenuItem(
            text = { Text("Downloads") },
            onClick = onDownloadsClick
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = onSettingsClick
        )
    }
}
