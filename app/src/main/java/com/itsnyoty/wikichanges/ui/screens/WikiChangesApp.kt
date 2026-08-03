package com.itsnyoty.wikichanges.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object RecentChanges : Screen("recent_changes")
    object Settings : Screen("settings")
}

@Composable
fun WikiChangesApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Landing.route) {
        composable(Screen.Landing.route) {
            LandingScreen(
                onSkip = {
                    navController.navigate(Screen.RecentChanges.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                    }
                },
                onAuthenticated = {
                    navController.navigate(Screen.RecentChanges.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.RecentChanges.route) {
            RecentChangesScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
