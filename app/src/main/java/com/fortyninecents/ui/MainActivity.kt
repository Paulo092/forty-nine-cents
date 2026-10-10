package com.fortyninecents.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fortyninecents.ui.navigation.AppNavHost
import com.fortyninecents.ui.theme.FortyNineCentsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FortyNineCentsTheme {
                AppNavHost()
            }
        }
    }
}
