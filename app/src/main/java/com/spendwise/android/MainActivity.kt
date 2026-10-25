package com.spendwise.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendwise.android.notification.SpendoNotificationHelper
import com.spendwise.android.notification.SpendoReminderScheduler
import com.spendwise.android.ui.spendo.SpendoApp
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.theme.SpendoTheme

/**
 * Main Activity and primary entry point for Spendo — Your AI Money Agent.
 */
class MainActivity : ComponentActivity() {

    private val pendingRouteState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Graceful handling: user either granted or denied; no crashes.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channel and schedule alarms
        SpendoNotificationHelper.createNotificationChannel(this)
        SpendoReminderScheduler.scheduleAll(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        handleIntent(intent)

        setContent {
            val viewModel: SpendoViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val pendingRoute by pendingRouteState

            SpendoTheme(themeMode = uiState.themeMode) {
                SpendoApp(
                    viewModel = viewModel,
                    initialRoute = pendingRoute,
                    onRouteConsumed = { pendingRouteState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val target = intent.getStringExtra("navigate_to")
        if (!target.isNullOrBlank()) {
            pendingRouteState.value = target
        }
    }
}

