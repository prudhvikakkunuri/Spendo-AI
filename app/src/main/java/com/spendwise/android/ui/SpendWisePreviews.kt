package com.spendwise.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.spendwise.android.ui.budget.BudgetScreen
import com.spendwise.android.ui.dashboard.DashboardScreen
import com.spendwise.android.ui.onboarding.OnboardingScreen
import com.spendwise.android.ui.review.ReviewScreen
import com.spendwise.android.ui.settings.SettingsScreen
import com.spendwise.android.ui.theme.SpendWiseTheme
import com.spendwise.android.ui.transactions.AddEditTransactionScreen
import com.spendwise.android.ui.transactions.TransactionsScreen

@Preview(name = "1. Dashboard Screen", showBackground = true)
@Composable
fun DashboardPreview() {
    SpendWiseTheme {
        DashboardScreen(
            onNavigateToBudget = {},
            onNavigateToTransactions = {},
            onNavigateToReview = {},
            onNavigateToAddTransaction = {},
            onNavigateToNotificationSetup = {}
        )
    }
}

@Preview(name = "2. Budget & Safe-to-Spend Screen", showBackground = true)
@Composable
fun BudgetPreview() {
    SpendWiseTheme {
        BudgetScreen(
            onNavigateBack = {}
        )
    }
}

@Preview(name = "3. Transactions Screen", showBackground = true)
@Composable
fun TransactionsPreview() {
    SpendWiseTheme {
        TransactionsScreen(
            onNavigateToAddTransaction = {},
            onNavigateToEditTransaction = {}
        )
    }
}

@Preview(name = "4. Add / Edit Transaction Screen", showBackground = true)
@Composable
fun AddEditTransactionPreview() {
    SpendWiseTheme {
        AddEditTransactionScreen(
            onNavigateBack = {}
        )
    }
}

@Preview(name = "5. Review Queue Screen", showBackground = true)
@Composable
fun ReviewPreview() {
    SpendWiseTheme {
        ReviewScreen(
            onNavigateToEditCandidate = {}
        )
    }
}

@Preview(name = "6. Settings Screen", showBackground = true)
@Composable
fun SettingsPreview() {
    SpendWiseTheme {
        SettingsScreen(
            onNavigateToNotificationSetup = {},
            onNavigateToMonitoredApps = {}
        )
    }
}

@Preview(name = "7. Onboarding Screen", showBackground = true)
@Composable
fun OnboardingPreview() {
    SpendWiseTheme {
        OnboardingScreen(
            onContinueToApp = {},
            onSetupNotifications = {}
        )
    }
}

@Preview(name = "8. Notification Setup Screen", showBackground = true)
@Composable
fun NotificationSetupPreview() {
    SpendWiseTheme {
        com.spendwise.android.ui.notification.NotificationSetupScreen(
            onNavigateBack = {},
            onNavigateToMonitoredApps = {}
        )
    }
}

@Preview(name = "9. Monitored Apps Screen", showBackground = true)
@Composable
fun MonitoredAppsPreview() {
    SpendWiseTheme {
        com.spendwise.android.ui.notification.MonitoredAppsScreen(
            onNavigateBack = {}
        )
    }
}
