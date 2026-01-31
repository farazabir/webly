package com.farazinc.webly.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farazinc.webly.ui.viewmodel.ReaderModeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeScreen(
    url: String,
    html: String,
    onNavigateBack: () -> Unit,
    viewModel: ReaderModeViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
    modifier: Modifier = Modifier
) {
    val article by viewModel.article.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    var showFontMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(url, html) {
        viewModel.parseArticle(html, url)
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                title = { Text("Reader Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Reader Mode"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFontMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Text size"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showFontMenu,
                            onDismissRequest = { showFontMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Small") },
                                onClick = {
                                    viewModel.setFontSize(14)
                                    showFontMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Medium") },
                                onClick = {
                                    viewModel.setFontSize(16)
                                    showFontMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Large") },
                                onClick = {
                                    viewModel.setFontSize(18)
                                    showFontMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Extra Large") },
                                onClick = {
                                    viewModel.setFontSize(20)
                                    showFontMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                article != null -> {
                    ArticleContent(
                        article = article!!,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to extract article",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This page may not be supported in Reader Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleContent(
    article: com.farazinc.webly.domain.ReaderModeParser.Article,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (fontSize + 8).sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (article.byline != null || article.siteName != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                article.byline?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = (fontSize - 2).sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (article.byline != null && article.siteName != null) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = (fontSize - 2).sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                article.siteName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = (fontSize - 2).sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Divider(modifier = Modifier.padding(bottom = 16.dp))
        
        val styledText = remember(article.content) {
            HtmlCompat.fromHtml(article.content, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
        
        Text(
            text = styledText.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize.sp,
                lineHeight = (fontSize + 8).sp
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
