package com.spendwise.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendwise.android.ui.spendo.SpendoApp
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.theme.SpendoTheme

/**
 * Main Activity and primary entry point for Spendo — Your AI Money Agent.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SpendoViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            SpendoTheme(themeMode = uiState.themeMode) {
                SpendoApp(viewModel = viewModel)
            }
        }
    }
}
