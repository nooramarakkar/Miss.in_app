package com.missin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.missin.app.ui.screens.auth.AuthScreen
import com.missin.app.ui.theme.MissInTheme
import com.missin.app.ui.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Injected via Hilt — auth check runs synchronously in SplashViewModel.init{}
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install before super so the OS splash is shown during activity init
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 2. Keep the native splash on screen until the auth check is done.
        //    SplashViewModel.init{} flips isLoading to false synchronously,
        //    so this releases the splash on the very first frame draw.
        splashScreen.setKeepOnScreenCondition { splashViewModel.isLoading.value }

        // 3. Edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MissInTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // startDest is pre-computed in SplashViewModel — no recomposition needed
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = splashViewModel.startDest
                    ) {
                        composable("auth") {
                            AuthScreen(
                                onAuthSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onProfileSetupNeeded = {
                                    navController.navigate("profile_setup") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("profile_setup") {
                            com.missin.app.ui.screens.auth.ProfileSetupScreen(
                                onProfileComplete = {
                                    navController.navigate("dashboard") {
                                        popUpTo("profile_setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            com.missin.app.ui.screens.dashboard.DashboardScreen(
                                onLogout = {
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToReport = { isFound ->
                                    val route = if (isFound) "report_found" else "report_lost"
                                    navController.navigate(route)
                                },
                                onEditProfile = {
                                    navController.navigate("profile_edit")
                                },
                                onNavigateToClaim = { foundItemId, finderId ->
                                    navController.navigate("proof_of_ownership/$foundItemId/$finderId")
                                },
                                onNavigateToIncomingClaims = {
                                    navController.navigate("claims_dashboard")
                                },
                                onNavigateToYourReports = {
                                    navController.navigate("your_reports")
                                },
                                onNavigateToChat = { claimRequestId ->
                                    navController.navigate("chat/$claimRequestId")
                                },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = it }
                            )
                        }
                        composable("profile_edit") {
                            com.missin.app.ui.screens.auth.ProfileSetupScreen(
                                onProfileComplete = {
                                    navController.popBackStack()
                                },
                                isEdit = true
                            )
                        }
                        composable("report_found") {
                            com.missin.app.ui.screens.dashboard.ReportFormsScreen(
                                isFoundForm = true,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("report_lost") {
                            com.missin.app.ui.screens.dashboard.ReportFormsScreen(
                                isFoundForm = false,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("proof_of_ownership/{foundItemId}/{finderId}") { backStackEntry ->
                            val foundItemId = backStackEntry.arguments?.getString("foundItemId") ?: ""
                            val finderId = backStackEntry.arguments?.getString("finderId") ?: ""
                            com.missin.app.ui.screens.dashboard.ProofOfOwnershipScreen(
                                foundItemId = foundItemId,
                                finderId = finderId,
                                onBack = { navController.popBackStack() },
                                onComplete = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("claims_dashboard") {
                            com.missin.app.ui.screens.dashboard.ClaimValidationScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToChat = { claimId ->
                                    navController.navigate("chat/$claimId")
                                }
                            )
                        }
                        composable("your_reports") {
                            com.missin.app.ui.screens.dashboard.YourReportsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("chat/{claimId}") { backStackEntry ->
                            val claimId = backStackEntry.arguments?.getString("claimId") ?: ""
                            com.missin.app.ui.screens.dashboard.ChatScreen(
                                claimId = claimId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("match_detail/{theirLat}/{theirLng}/{myLat}/{myLng}") { backStackEntry ->
                            val theirLat = backStackEntry.arguments?.getString("theirLat")?.toDoubleOrNull() ?: 0.0
                            val theirLng = backStackEntry.arguments?.getString("theirLng")?.toDoubleOrNull() ?: 0.0
                            val myLat = backStackEntry.arguments?.getString("myLat")?.toDoubleOrNull() ?: 0.0
                            val myLng = backStackEntry.arguments?.getString("myLng")?.toDoubleOrNull() ?: 0.0

                            com.missin.app.ui.screens.dashboard.MatchDetailScreen(
                                theirLat = theirLat,
                                theirLng = theirLng,
                                myLat = myLat,
                                myLng = myLng,
                                onBack = { navController.popBackStack() },
                                onStartVerification = {
                                    // Handle verification navigation here later
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
