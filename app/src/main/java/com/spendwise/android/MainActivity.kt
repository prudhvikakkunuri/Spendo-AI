package com.spendwise.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.spendwise.android.ui.spendo.SpendoApp
import com.spendwise.android.ui.spendo.theme.SpendoTheme

/**
 * Main Activity and primary entry point for Spendo — Your AI Money Agent.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendoTheme {
                SpendoApp()
            }
        }
    }
}
