package com.spendwise.android.ui.spendo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendwise.android.ui.spendo.components.SpendoBottomBar
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.navigation.SpendoNavHost
import com.spendwise.android.ui.spendo.navigation.SpendoScreen
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoApp(
    viewModel: SpendoViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: SpendoScreen.Home.route

    val uiState by viewModel.uiState.collectAsState()

    val bottomBarRoutes = setOf(
        SpendoScreen.Home.route,
        SpendoScreen.Activity.route,
        SpendoScreen.Calendar.route,
        SpendoScreen.SpendoAi.route
    )
    val isBottomBarVisible = currentRoute in bottomBarRoutes

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
                    currentRoute = currentRoute,
                    onNavigateToDestination = { targetRoute ->
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
            modifier = Modifier.padding(innerPadding)
        )
    }
}
