package com.spendwise.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendwise.android.SpendWiseApplication
import com.spendwise.android.data.db.AppDatabase
import com.spendwise.android.data.repository.BudgetRepository
import com.spendwise.android.data.repository.TransactionRepository
import com.spendwise.android.ui.budget.BudgetScreen
import com.spendwise.android.ui.budget.BudgetViewModel
import com.spendwise.android.ui.dashboard.DashboardScreen
import com.spendwise.android.ui.dashboard.DashboardViewModel
import com.spendwise.android.ui.navigation.Screen
import com.spendwise.android.ui.notification.MonitoredAppsScreen
import com.spendwise.android.ui.notification.NotificationSetupScreen
import com.spendwise.android.ui.onboarding.OnboardingScreen
import com.spendwise.android.ui.review.ReviewScreen
import com.spendwise.android.ui.settings.SettingsScreen
import com.spendwise.android.ui.theme.CharcoalBorderSubtle
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CharcoalSurface
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextPrimary
import com.spendwise.android.ui.transactions.AddEditTransactionScreen
import com.spendwise.android.ui.transactions.AddEditTransactionViewModel
import com.spendwise.android.ui.transactions.TransactionsScreen
import com.spendwise.android.ui.transactions.TransactionsViewModel

@Composable
fun SpendWiseApp(
    repository: TransactionRepository? = null,
    budgetRepository: BudgetRepository? = null
) {
    val context = LocalContext.current.applicationContext
    val activeRepository = repository ?: (context as? SpendWiseApplication)?.transactionRepository
        ?: remember {
            val db = AppDatabase.getInstance(context)
            TransactionRepository(db.transactionDao())
        }
    val activeBudgetRepository = budgetRepository ?: (context as? SpendWiseApplication)?.budgetRepository
        ?: remember {
            val db = AppDatabase.getInstance(context)
            BudgetRepository(
                budgetSettingsDao = db.budgetSettingsDao(),
                commitmentDao = db.commitmentDao(),
                transactionDao = db.transactionDao()
            )
        }

    val monitoredAppRepository = (context as? SpendWiseApplication)?.monitoredAppRepository
        ?: remember {
            val db = AppDatabase.getInstance(context)
            com.spendwise.android.data.repository.MonitoredAppRepository(db.monitoredAppDao())
        }

    val notificationSettingsRepository = (context as? SpendWiseApplication)?.notificationSettingsRepository
        ?: remember {
            com.spendwise.android.data.repository.NotificationSettingsRepository(context)
        }

    val notificationAccessManager = (context as? SpendWiseApplication)?.notificationAccessManager
        ?: remember {
            com.spendwise.android.notification.NotificationAccessManager(context, notificationSettingsRepository)
        }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Review.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = CharcoalBorderSubtle
                    )
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        contentColor = SlateTextPrimary,
                        tonalElevation = 0.dp
                    ) {
                    Screen.bottomNavItems.filterNotNull().forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CharcoalDark,
                                selectedTextColor = LimeAccent,
                                indicatorColor = LimeAccent,
                                unselectedIconColor = SlateTextMuted,
                                unselectedTextColor = SlateTextMuted
                            )
                        )
                    }
                }
            }
        }
    }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onContinueToApp = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onSetupNotifications = {
                        navController.navigate(Screen.NotificationSetup.route)
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(activeRepository, activeBudgetRepository)
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    accessManager = notificationAccessManager,
                    onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToReview = { navController.navigate(Screen.Review.route) },
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddEditTransaction.createRoute())
                    },
                    onNavigateToNotificationSetup = {
                        navController.navigate(Screen.NotificationSetup.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                val transactionsViewModel: TransactionsViewModel = viewModel(
                    factory = TransactionsViewModel.provideFactory(activeRepository)
                )
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddEditTransaction.createRoute())
                    },
                    onNavigateToEditTransaction = { transactionId ->
                        navController.navigate(Screen.AddEditTransaction.createRoute(transactionId))
                    }
                )
            }

            composable(
                route = Screen.AddEditTransaction.route,
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")
                val addEditViewModel: AddEditTransactionViewModel = viewModel(
                    key = transactionId ?: "new_transaction",
                    factory = AddEditTransactionViewModel.provideFactory(activeRepository, transactionId)
                )
                AddEditTransactionScreen(
                    viewModel = addEditViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Review.route) {
                val reviewViewModel: com.spendwise.android.ui.review.ReviewViewModel = viewModel(
                    factory = com.spendwise.android.ui.review.ReviewViewModel.provideFactory(activeRepository)
                )
                ReviewScreen(
                    viewModel = reviewViewModel,
                    onNavigateToEditCandidate = { candidateId ->
                        navController.navigate(Screen.AddEditTransaction.createRoute(candidateId))
                    }
                )
            }

            composable(Screen.Budget.route) {
                val budgetViewModel: BudgetViewModel = viewModel(
                    factory = BudgetViewModel.provideFactory(activeBudgetRepository)
                )
                BudgetScreen(
                    viewModel = budgetViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    repository = activeRepository,
                    budgetRepository = activeBudgetRepository,
                    onNavigateToNotificationSetup = {
                        navController.navigate(Screen.NotificationSetup.route)
                    },
                    onNavigateToMonitoredApps = {
                        navController.navigate(Screen.MonitoredApps.route)
                    }
                )
            }

            composable(Screen.NotificationSetup.route) {
                NotificationSetupScreen(
                    accessManager = notificationAccessManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMonitoredApps = {
                        navController.navigate(Screen.MonitoredApps.route)
                    }
                )
            }

            composable(Screen.MonitoredApps.route) {
                val monitoredAppsViewModel: com.spendwise.android.ui.notification.MonitoredAppsViewModel = viewModel(
                    factory = com.spendwise.android.ui.notification.MonitoredAppsViewModel.provideFactory(
                        monitoredAppRepository = monitoredAppRepository,
                        settingsRepository = notificationSettingsRepository
                    )
                )
                MonitoredAppsScreen(
                    viewModel = monitoredAppsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
