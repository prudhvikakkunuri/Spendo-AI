package com.spendwise.android.ui.spendo.navigation

/**
 * Route identifiers for all 17 Spendo screens.
 */
sealed class SpendoScreen(val route: String) {
    // 4 Bottom Nav Destinations
    object Home : SpendoScreen("spendo_home")
    object Activity : SpendoScreen("spendo_activity")
    object Calendar : SpendoScreen("spendo_calendar")
    object SpendoAi : SpendoScreen("spendo_ai_chat")

    // Secondary Feature Screens
    object Notifications : SpendoScreen("spendo_notifications")
    object AddTransaction : SpendoScreen("spendo_add_transaction")
    object TransactionDetail : SpendoScreen("spendo_transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "spendo_transaction_detail/$transactionId"
    }
    object Budget : SpendoScreen("spendo_budget")
    object Analytics : SpendoScreen("spendo_analytics")
    object Banks : SpendoScreen("spendo_banks")
    object Income : SpendoScreen("spendo_income")
    object Streaks : SpendoScreen("spendo_streaks")
    object AiConfig : SpendoScreen("spendo_ai_config")
    object NotificationSettings : SpendoScreen("spendo_notification_settings")
    object ImportExport : SpendoScreen("spendo_import_export")
    object Settings : SpendoScreen("spendo_settings")
    object Security : SpendoScreen("spendo_security")
}
