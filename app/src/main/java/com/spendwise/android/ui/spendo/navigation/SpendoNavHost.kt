package com.spendwise.android.ui.spendo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.screens.SpendoActivityScreen
import com.spendwise.android.ui.spendo.screens.SpendoAddTransactionScreen
import com.spendwise.android.ui.spendo.screens.SpendoAiChatScreen
import com.spendwise.android.ui.spendo.screens.SpendoAiConfigScreen
import com.spendwise.android.ui.spendo.screens.SpendoAnalyticsScreen
import com.spendwise.android.ui.spendo.screens.SpendoBanksScreen
import com.spendwise.android.ui.spendo.screens.SpendoBudgetScreen
import com.spendwise.android.ui.spendo.screens.SpendoCalendarScreen
import com.spendwise.android.ui.spendo.screens.SpendoChangePasswordScreen
import com.spendwise.android.ui.spendo.screens.SpendoHomeScreen
import com.spendwise.android.ui.spendo.screens.SpendoImportExportScreen
import com.spendwise.android.ui.spendo.screens.SpendoIncomeScreen
import com.spendwise.android.ui.spendo.screens.SpendoNotificationScreen
import com.spendwise.android.ui.spendo.screens.SpendoNotificationSettingsScreen
import com.spendwise.android.ui.spendo.screens.SpendoReviewScreen
import com.spendwise.android.ui.spendo.screens.SpendoSecurityScreen
import com.spendwise.android.ui.spendo.screens.SpendoSettingsScreen
import com.spendwise.android.ui.spendo.screens.SpendoStreaksScreen
import com.spendwise.android.ui.spendo.screens.SpendoTransactionDetailScreen

@Composable
fun SpendoNavHost(
    navController: NavHostController,
    uiState: SpendoUiState,
    viewModel: SpendoViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = SpendoScreen.Home.route,
        modifier = modifier
    ) {
        // SCREEN 1 — HOME DASHBOARD
        composable(SpendoScreen.Home.route) {
            SpendoHomeScreen(
                uiState = uiState,
                onNavigateToAddTransaction = { navController.navigate(SpendoScreen.AddTransaction.route) },
                onNavigateToAddIncome = { navController.navigate(SpendoScreen.AddTransaction.route) },
                onNavigateToBanks = { navController.navigate(SpendoScreen.Banks.route) },
                onNavigateToAnalytics = { navController.navigate(SpendoScreen.Analytics.route) },
                onNavigateToSettings = { navController.navigate(SpendoScreen.Settings.route) },
                onNavigateToNotifications = { navController.navigate(SpendoScreen.Notifications.route) },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(SpendoScreen.TransactionDetail.createRoute(id))
                },
                onNavigateToActivity = { navController.navigate(SpendoScreen.Activity.route) },
                onNavigateToReview = { navController.navigate(SpendoScreen.Review.route) }
            )
        }

        // SCREEN 2 — ACTIVITY / ALL TRANSACTIONS
        composable(SpendoScreen.Activity.route) {
            SpendoActivityScreen(
                uiState = uiState,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onFilterTypeChange = { viewModel.setFilterType(it) },
                onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                onNavigateToAddTransaction = { navController.navigate(SpendoScreen.AddTransaction.route) },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(SpendoScreen.TransactionDetail.createRoute(id))
                }
            )
        }

        // SCREEN 3 — MONTHLY CALENDAR
        composable(SpendoScreen.Calendar.route) {
            SpendoCalendarScreen(
                uiState = uiState,
                onSelectDay = { viewModel.selectCalendarDay(it) },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(SpendoScreen.TransactionDetail.createRoute(id))
                }
            )
        }

        // SCREEN 4 — NOTIFICATION CENTRE
        composable(SpendoScreen.Notifications.route) {
            SpendoNotificationScreen(
                uiState = uiState,
                onCategorySelected = { viewModel.setNotificationCategory(it) },
                onClearAll = { viewModel.clearAllNotifications() },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { navController.navigate(SpendoScreen.Review.route) }
            )
        }

        // SCREEN 5 — ADD EXPENSE / INCOME
        composable(SpendoScreen.AddTransaction.route) {
            SpendoAddTransactionScreen(
                banks = uiState.banks,
                initialType = TransactionType.EXPENSE,
                onSaveTransaction = { merchant, amountPaise, type, category, bankName, note ->
                    viewModel.addTransaction(merchant, amountPaise, type, category, bankName, note)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 6 — TRANSACTION DETAILS
        composable(
            route = SpendoScreen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            SpendoTransactionDetailScreen(
                transactionId = transactionId,
                transactions = uiState.transactions,
                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 7 — BUDGET MANAGEMENT
        composable(SpendoScreen.Budget.route) {
            SpendoBudgetScreen(
                uiState = uiState,
                onUpdateLimits = { daily, monthly -> viewModel.updateBudgetLimits(daily, monthly) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 8 — SPENDING ANALYTICS
        composable(SpendoScreen.Analytics.route) {
            SpendoAnalyticsScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onEvaluateAffordability = { viewModel.evaluateAffordability(it) }
            )
        }

        // SCREEN 9 — BANKS OVERVIEW
        composable(SpendoScreen.Banks.route) {
            SpendoBanksScreen(
                uiState = uiState,
                onToggleUnlockDialog = { viewModel.toggleUnlockDialog(it) },
                onVerifyAndUnlock = { password, onSuccess, onError ->
                    viewModel.verifyAndUnlockBalances(password, onSuccess, onError)
                },
                onHideBalances = { viewModel.hideBalances() },
                onAddBank = { name, type, initial -> viewModel.addBank(name, type, initial) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 10 — INCOME AND SALARY
        composable(SpendoScreen.Income.route) {
            SpendoIncomeScreen(
                uiState = uiState,
                onNavigateToAddIncome = { navController.navigate(SpendoScreen.AddTransaction.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 11 — STREAKS AND ACHIEVEMENTS
        composable(SpendoScreen.Streaks.route) {
            SpendoStreaksScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 12 — SPENDO AI CHAT
        composable(SpendoScreen.SpendoAi.route) {
            SpendoAiChatScreen(
                uiState = uiState,
                onInputChange = { viewModel.setChatInput(it) },
                onSendMessage = { viewModel.sendChatMessage(it) },
                onNavigateToAiConfig = { navController.navigate(SpendoScreen.AiConfig.route) },
                onConfirmExpense = { pending, msgId -> viewModel.confirmPendingExpense(pending, msgId) },
                onDismissExpense = { msgId -> viewModel.dismissPendingExpense(msgId) }
            )
        }

        // SCREEN 13 — AI PROVIDER CONFIGURATION
        composable(SpendoScreen.AiConfig.route) {
            SpendoAiConfigScreen(
                aiConfig = uiState.aiConfig,
                onSelectProvider = { viewModel.setAiProvider(it) },
                onApiKeyChange = { viewModel.setAiApiKey(it) },
                onModelChange = { viewModel.setAiModel(it) },
                onTestConnection = { viewModel.testAiConnection() },
                onSaveConfig = { viewModel.saveAiConfiguration() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 14 — NOTIFICATION DETECTION SETTINGS
        composable(SpendoScreen.NotificationSettings.route) {
            SpendoNotificationSettingsScreen(
                uiState = uiState,
                onToggleDetection = { viewModel.toggleNotificationDetection(it) },
                onToggleApp = { viewModel.toggleAppMonitored(it) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 15 — IMPORT / EXPORT
        composable(SpendoScreen.ImportExport.route) {
            SpendoImportExportScreen(
                totalTransactionsCount = uiState.transactions.size,
                onNavigateBack = { navController.popBackStack() },
                onImportCsv = { stream, onDone -> viewModel.importCsv(stream, onDone) }
            )
        }

        // SCREEN 16 — SETTINGS
        composable(SpendoScreen.Settings.route) {
            SpendoSettingsScreen(
                currentThemeMode = uiState.themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onNavigateToSecurity = { navController.navigate(SpendoScreen.Security.route) },
                onNavigateToBanks = { navController.navigate(SpendoScreen.Banks.route) },
                onNavigateToBudgets = { navController.navigate(SpendoScreen.Budget.route) },
                onNavigateToIncome = { navController.navigate(SpendoScreen.Income.route) },
                onNavigateToStreaks = { navController.navigate(SpendoScreen.Streaks.route) },
                onNavigateToAiConfig = { navController.navigate(SpendoScreen.AiConfig.route) },
                onNavigateToNotifications = { navController.navigate(SpendoScreen.NotificationSettings.route) },
                onNavigateToImportExport = { navController.navigate(SpendoScreen.ImportExport.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 17 — SECURITY SETUP
        composable(SpendoScreen.Security.route) {
            SpendoSecurityScreen(
                uiState = uiState,
                onToggleBiometric = { viewModel.setBiometricEnabled(it) },
                onAutoLockChange = { viewModel.setAutoLockDuration(it) },
                onToggleBalanceConceal = { viewModel.setBalanceConcealProtected(it) },
                onNavigateToChangePassword = { navController.navigate(SpendoScreen.ChangePassword.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 17b — CHANGE PASSWORD SCREEN
        composable(SpendoScreen.ChangePassword.route) {
            SpendoChangePasswordScreen(
                hasCustomPassword = uiState.hasCustomPassword,
                onChangePassword = { current, newPass ->
                    viewModel.changePassword(current, newPass)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SCREEN 18 — REVIEW / VERIFICATION DESK
        composable(SpendoScreen.Review.route) {
            SpendoReviewScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onApprove = { id -> viewModel.approveReviewTransaction(id) },
                onReject = { id -> viewModel.rejectReviewTransaction(id) },
                onUpdateAndApprove = { id, merchant, amountPaise, category, bank, rememberPref ->
                    viewModel.updateAndApproveReviewTransaction(id, merchant, amountPaise, category, bank, rememberPref)
                }
            )
        }
    }
}
