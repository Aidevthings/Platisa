package com.platisa.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.platisa.app.ui.screens.analytics.AnalyticsScreen
import com.platisa.app.ui.screens.analytics.StatisticsScreen
import com.platisa.app.ui.screens.home.HomeScreen
import com.platisa.app.ui.screens.settings.SettingsScreen
import com.platisa.app.ui.screens.billdetails.BillDetailsScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlatisaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            com.platisa.app.ui.screens.splash.SplashScreen(navController = navController)
        }
        composable(Screen.Greetings.route) {
            com.platisa.app.ui.screens.splash.GreetingsScreen(navController = navController)
        }

        composable(
            route = Screen.Login.route,
            arguments = listOf(
                androidx.navigation.navArgument("autoLogin") {
                    type = androidx.navigation.NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val autoLogin = backStackEntry.arguments?.getBoolean("autoLogin") ?: false
            com.platisa.app.ui.screens.login.LoginScreen(
                navController = navController,
                autoLogin = autoLogin
            )
        }
        composable(Screen.ScanTimeframe.route) {
            com.platisa.app.ui.screens.onboarding.ScanTimeframeScreen(navController = navController)
        }
        composable(Screen.SubscriptionPaywall.route) {
            com.platisa.app.ui.screens.subscription.SubscriptionPaywallScreen(navController = navController)
        }
        composable(Screen.SyncWait.route) {
            com.platisa.app.ui.screens.onboarding.SyncWaitScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Market.route) {
            com.platisa.app.ui.screens.comparison.ComparisonScreen(navController = navController)
        }
        composable(Screen.Analytics.route) {
            StatisticsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            com.platisa.app.ui.screens.profile.ProfileScreen(navController = navController)
        }
        composable(Screen.Help.route) {
            com.platisa.app.ui.screens.help.HelpScreen(navController = navController)
        }
        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                androidx.navigation.navArgument("openSection") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val openSection = backStackEntry.arguments?.getString("openSection")
            SettingsScreen(
                navController = navController,
                openSection = openSection
            )
        }
        composable(Screen.Camera.route) {
            com.platisa.app.ui.screens.camera.CameraScreen(navController = navController)
        }
        composable(
            route = Screen.ReviewReceipt.route,
            arguments = listOf(androidx.navigation.navArgument("imageUri") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            com.platisa.app.ui.screens.review.ReviewReceiptScreen(navController = navController, imageUri = imageUri)
        }

        composable(Screen.EpsAnalytics.route) {
            val viewModel: com.platisa.app.ui.screens.analytics.EpsAnalyticsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val monthlyData by viewModel.monthlyData.collectAsState()
            
            com.platisa.app.ui.screens.analytics.EpsAnalyticsScreen(monthlyData = monthlyData)
        }
        composable(Screen.Search.route) {
            com.platisa.app.ui.screens.search.SearchScreen(navController = navController)
        }
        composable(Screen.Diagnostics.route) {
            com.platisa.app.ui.screens.diagnostics.DiagnosticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ReceiptDetail.route,
            arguments = listOf(androidx.navigation.navArgument("receiptId") { type = androidx.navigation.NavType.LongType })
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
            BillDetailsScreen(
                navController = navController,
                billId = receiptId.toString()
            )
        }
        composable(
            route = Screen.BillDetails.route,
            arguments = listOf(androidx.navigation.navArgument("billId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: ""
            BillDetailsScreen(
                navController = navController,
                billId = billId
            )
        }
        composable(
            route = Screen.FiscalReceiptDetails.route,
            arguments = listOf(androidx.navigation.navArgument("receiptId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getString("receiptId") ?: ""
            com.platisa.app.ui.screens.fiscaldetails.FiscalReceiptDetailsScreen(
                navController = navController,
                receiptId = receiptId
            )
        }
        composable(Screen.Walkthrough.route) {
            com.platisa.app.ui.screens.help.WalkthroughScreen(navController = navController)
        }
    }
}

