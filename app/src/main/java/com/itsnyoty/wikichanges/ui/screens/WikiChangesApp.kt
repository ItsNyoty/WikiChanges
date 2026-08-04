package com.itsnyoty.wikichanges.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itsnyoty.wikichanges.R
import com.itsnyoty.wikichanges.data.repository.WikipediaRepository
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object RecentChanges : Screen("recent_changes")
    object Settings : Screen("settings")
    object DeveloperSettings : Screen("developer_settings")
}

@Composable
fun WikiChangesApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { WikipediaRepository.getInstance(context) }
    val isDisclaimerAccepted by repository.isDisclaimerAccepted.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    if (isDisclaimerAccepted == false) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = { 
                Text(stringResource(R.string.disclaimer_text)) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.setDisclaimerAccepted()
                        }
                    }
                ) {
                    Text(stringResource(R.string.disclaimer_agree))
                }
            }
        )
    }

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
                },
                onNavigateToDeveloperSettings = {
                    navController.navigate(Screen.DeveloperSettings.route)
                }
            )
        }
        composable(Screen.RecentChanges.route) {
            RecentChangesScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDeveloperSettings = {
                    navController.navigate(Screen.DeveloperSettings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DeveloperSettings.route) {
            DeveloperSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
