package com.spendwise.android.ui.spendo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendwise.android.ui.spendo.components.SpendoBottomBar
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.navigation.SpendoNavHost
import com.spendwise.android.ui.spendo.navigation.SpendoScreen
import com.spendwise.android.ui.spendo.theme.SpendoColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpendoApp(
    viewModel: SpendoViewModel = viewModel(),
    initialRoute: String? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: SpendoScreen.Home.route
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            when (initialRoute) {
                "activity" -> {
                    if (currentRoute != SpendoScreen.Home.route) {
                        navController.popBackStack(SpendoScreen.Home.route, inclusive = false)
                    }
                    pagerState.scrollToPage(1)
                }
                "analytics" -> {
                    if (currentRoute != SpendoScreen.Home.route) {
                        navController.popBackStack(SpendoScreen.Home.route, inclusive = false)
                    }
                    pagerState.scrollToPage(2)
                }
                "budget", "budgets" -> {
                    if (currentRoute != SpendoScreen.Home.route) {
                        navController.popBackStack(SpendoScreen.Home.route, inclusive = false)
                    }
                    pagerState.scrollToPage(3)
                }
                "settings", "profile" -> {
                    if (currentRoute != SpendoScreen.Home.route) {
                        navController.popBackStack(SpendoScreen.Home.route, inclusive = false)
                    }
                    pagerState.scrollToPage(4)
                }
                "add_transaction" -> {
                    navController.navigate(SpendoScreen.AddTransaction.route) {
                        launchSingleTop = true
                    }
                }
                "voice_expense", "spendo_ai" -> {
                    navController.navigate(SpendoScreen.SpendoAi.route) {
                        launchSingleTop = true
                    }
                }
                "banks" -> {
                    navController.navigate(SpendoScreen.Banks.route) {
                        launchSingleTop = true
                    }
                }
                "review", "spendo_review" -> {
                    navController.navigate(SpendoScreen.Review.route) {
                        launchSingleTop = true
                    }
                }
                "calendar" -> {
                    navController.navigate(SpendoScreen.Calendar.route) {
                        launchSingleTop = true
                    }
                }
            }
            onRouteConsumed()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    // The bottom bar is visible across all 5 main pages on the main pager.
    // It slides out when entering dedicated focus screens like Onboarding, Add Transaction, Detail, or Spendo AI.
    val hiddenBarRoutes = setOf(
        SpendoScreen.Onboarding.route,
        SpendoScreen.AddTransaction.route,
        SpendoScreen.TransactionDetail.route,
        SpendoScreen.SpendoAi.route
    )
    val isBottomBarVisible = currentRoute !in hiddenBarRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SpendoColor.Background,
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                SpendoBottomBar(
                    selectedPageIndex = pagerState.currentPage,
                    onSelectPage = { pageIndex ->
                        if (currentRoute != SpendoScreen.Home.route) {
                            val popped = navController.popBackStack(SpendoScreen.Home.route, inclusive = false)
                            if (!popped) {
                                navController.navigate(SpendoScreen.Home.route) {
                                    popUpTo(SpendoScreen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                        if (pagerState.currentPage != pageIndex) {
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(pageIndex)
                                } catch (_: Exception) {
                                    pagerState.scrollToPage(pageIndex)
                                }
                            }
                        }
                    },
                    onSpendoAiClick = {
                        navController.navigate(SpendoScreen.SpendoAi.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        SpendoNavHost(
            navController = navController,
            uiState = uiState,
            viewModel = viewModel,
            pagerState = pagerState,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
