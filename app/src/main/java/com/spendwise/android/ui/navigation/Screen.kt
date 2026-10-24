package com.spendwise.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes and destinations for SpendWise.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Assessment)
    object Transactions : Screen("transactions", "Transactions", Icons.AutoMirrored.Filled.ReceiptLong)
    object AddEditTransaction : Screen("add_edit_transaction?transactionId={transactionId}", "Transaction") {
        fun createRoute(transactionId: String? = null): String {
            return if (transactionId != null) "add_edit_transaction?transactionId=$transactionId" else "add_edit_transaction"
        }
    }
    object Review : Screen("review", "Review", Icons.Default.RateReview)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Budget : Screen("budget", "Budget", Icons.Default.AccountBalanceWallet)
    object NotificationSetup : Screen("notification_setup", "Notification Access", Icons.Default.NotificationsActive)
    object MonitoredApps : Screen("monitored_apps", "Monitored Apps", Icons.Default.Security)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Transactions, Review, Settings)
    }
}
