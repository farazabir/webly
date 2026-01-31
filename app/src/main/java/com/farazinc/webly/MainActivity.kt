package com.farazinc.webly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.farazinc.webly.ui.screen.BrowserApp
import com.farazinc.webly.ui.theme.WeblyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            WeblyTheme {
                BrowserApp(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}