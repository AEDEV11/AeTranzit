package com.aedev.aetranzit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aedev.aetranzit.ui.navigation.AeTranzitNavHost
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AeTranzitTheme {
                AeTranzitNavHost()
            }
        }
    }
}
