package com.spendwise.android.ui.spendo.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.model.TransactionType
import kotlinx.coroutines.launch

/**
 * Main 5-destination pager container supporting swipe navigation and
 * synchronized bottom navigation bar selection across:
 * 0: Home
 * 1: Wallet / Transactions
 * 2: Analytics
 * 3: Budgets
 * 4: Profile / Settings
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpendoMainPagerScreen(
    pagerState: PagerState,
    uiState: SpendoUiState,
    viewModel: SpendoViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToBanks: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToStreaks: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Handle back button on secondary pages: smoothly returns to Home (Page 0)
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondBoundsPageCount = 1,
        key = { it }
    ) { pageIndex ->
        when (pageIndex) {
            0 -> SpendoHomeScreen(
                uiState = uiState,
                onNavigateToAddTransaction = onNavigateToAddTransaction,
                onNavigateToAddIncome = onNavigateToAddIncome,
                onNavigateToBanks = onNavigateToBanks,
                onNavigateToAnalytics = {
                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                },
                onNavigateToSettings = {
                    coroutineScope.launch { pagerState.animateScrollToPage(4) }
                },
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                onNavigateToActivity = {
                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                onNavigateToReview = onNavigateToReview,
                onNavigateToCalendar = onNavigateToCalendar
            )

            1 -> SpendoActivityScreen(
                uiState = uiState,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onFilterTypeChange = { viewModel.setFilterType(it) },
                onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                onNavigateToAddTransaction = onNavigateToAddTransaction,
                onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                onNavigateToCalendar = onNavigateToCalendar
            )

            2 -> SpendoAnalyticsScreen(
                uiState = uiState,
                onNavigateBack = {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                },
                onEvaluateAffordability = { viewModel.evaluateAffordability(it) }
            )

            3 -> SpendoBudgetScreen(
                uiState = uiState,
                onUpdateLimits = { daily, monthly ->
                    viewModel.updateBudgetLimits(daily, monthly)
                },
                onNavigateBack = {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                }
            )

            4 -> SpendoSettingsScreen(
                currentThemeMode = uiState.themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                userName = uiState.userName,
                monthlyIncomePaise = uiState.monthlyIncomePaise,
                incomeFrequency = uiState.incomeFrequency,
                dailyLimitPaise = uiState.dailyLimitPaise,
                onUpdateProfile = { name, salary, freq, limit ->
                    viewModel.updateProfile(name, salary, freq, limit)
                },
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToBanks = onNavigateToBanks,
                onNavigateToBudgets = {
                    coroutineScope.launch { pagerState.animateScrollToPage(3) }
                },
                onNavigateToIncome = onNavigateToIncome,
                onNavigateToStreaks = onNavigateToStreaks,
                onNavigateToAiConfig = onNavigateToAiConfig,
                onNavigateToNotifications = onNavigateToNotificationSettings,
                onNavigateToImportExport = onNavigateToImportExport,
                onNavigateBack = {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                }
            )
        }
    }
}
