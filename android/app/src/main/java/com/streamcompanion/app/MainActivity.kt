package com.streamcompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.streamcompanion.app.ui.AppNav
import com.streamcompanion.app.ui.theme.Palette
import com.streamcompanion.app.ui.theme.StreamTheme

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamTheme {
                Surface(Modifier.fillMaxSize().background(Palette.Background), color = Palette.Background) { AppNav() }
            }
        }
    }
}
