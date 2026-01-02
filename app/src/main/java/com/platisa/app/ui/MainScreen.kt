package com.platisa.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.platisa.app.MainActivity
import com.platisa.app.ui.screens.help.tutorialTarget
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.platisa.app.core.common.SnackbarManager
import com.platisa.app.ui.navigation.PlatisaNavHost
import com.platisa.app.ui.navigation.Screen
import com.platisa.app.ui.theme.LocalPlatisaColors
import com.platisa.app.ui.theme.PlatisaCustomColors

// Navigation item color types - resolved at runtime based on theme
enum class NavColorType {
    STATUS_PROCESSING,  // Yellow/Gold
    STATUS_PAID,        // Green
    STATUS_UNPAID,      // Magenta/Pink
    NEON_CYAN           // Cyan
}

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val colorType: NavColorType
) {
    data object Home : BottomNavItem(Screen.Home.route, "Režije", Icons.Default.Receipt, NavColorType.STATUS_PROCESSING)
    data object Market : BottomNavItem(Screen.Market.route, "Market", Icons.Default.LocalGroceryStore, NavColorType.STATUS_PAID)
    data object Analytics : BottomNavItem(Screen.Analytics.route, "Podaci", Icons.Default.BarChart, NavColorType.STATUS_UNPAID)
    data object Settings : BottomNavItem(Screen.Settings.route, "Opcije", Icons.Default.Settings, NavColorType.NEON_CYAN)
}

// Extension function to get the actual color from theme
@Composable
private fun NavColorType.toColor(customColors: PlatisaCustomColors): Color {
    return when (this) {
        NavColorType.STATUS_PROCESSING -> customColors.navHome
        NavColorType.STATUS_PAID -> customColors.navMarket
        NavColorType.STATUS_UNPAID -> customColors.navAnalytics
        NavColorType.NEON_CYAN -> customColors.navSettings
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val customColors = LocalPlatisaColors.current
    
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Market,
        BottomNavItem.Analytics,
        BottomNavItem.Settings
    )

    LaunchedEffect(Unit) {
        SnackbarManager.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    // Deep-link navigation from notifications
    val pendingBillId by MainActivity.pendingBillId.collectAsState()
    LaunchedEffect(pendingBillId) {
        pendingBillId?.let { billId ->
            android.util.Log.d("MainScreen", "Navigating to bill details: $billId")
            navController.navigate(Screen.BillDetails.createRoute(billId.toString())) {
                // Don't add to back stack to avoid duplicate entries
                launchSingleTop = true
            }
            MainActivity.clearPendingBillId()
        }
    }
    
    // Deep-link to Settings (e.g., Auth Error)
    val pendingSettingsOpen by MainActivity.pendingSettingsOpen.collectAsState()
    LaunchedEffect(pendingSettingsOpen) {
        if (pendingSettingsOpen) {
             android.util.Log.d("MainScreen", "Navigating to Settings due to pending intent")
             navController.navigate(Screen.Settings.route) {
                 popUpTo(Screen.Home.route) {
                     saveState = true
                 }
                 launchSingleTop = true
                 restoreState = true
             }
             MainActivity.clearPendingSettingsOpen()
        }
    }
    
    
    // Tutorial state
    val showTutorial by com.platisa.app.ui.screens.help.TutorialState.showTutorial.collectAsState()
    LaunchedEffect(showTutorial) {
        android.util.Log.d("MainScreen", "Tutorial visibility changed: $showTutorial")
    }

    // Wrap everything in a Box for proper z-ordering of tutorial overlay
    Box(modifier = Modifier.fillMaxSize()) {
        // Determine if bottom nav should be visible based on current route
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // Routes that should NOT show bottom navigation
        val fullScreenRoutes = listOf(
            Screen.Login.route,
            Screen.SyncWait.route,
            Screen.ScanTimeframe.route,
            Screen.Splash.route,
            Screen.Greetings.route,
            Screen.SubscriptionPaywall.route
        )
        val shouldShowBottomBar = currentRoute?.let { route ->
            fullScreenRoutes.none { fullScreenRoute ->
                route.startsWith(fullScreenRoute.substringBefore("?"))
            }
        } ?: true
        
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                // Only show bottom nav for main app screens
                if (shouldShowBottomBar) {
                // Custom Bottom Navigation Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(NavigationBarDefaults.windowInsets) // Handle bottom system inset
                            .height(65.dp) // Total height of the clickable area
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentDestination = navBackStackEntry?.destination

                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val itemColor = item.colorType.toColor(customColors)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (item.route == Screen.Home.route) {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Home.route) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        } else {
                                            navController.navigate(item.route) {
                                                popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val isTutorialItem = item is BottomNavItem.Market || item is BottomNavItem.Analytics
                                val tutorialModifier = when (item) {
                                    is BottomNavItem.Market -> Modifier.tutorialTarget("pijaca_nav")
                                    is BottomNavItem.Analytics -> Modifier.tutorialTarget("statistics_nav")
                                    else -> Modifier
                                }

                                Box(modifier = tutorialModifier) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) itemColor else customColors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = item.label,
                                    color = if (isSelected) itemColor else customColors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                } // End if (shouldShowBottomBar)
            }
        ) { innerPadding ->
            PlatisaNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
        
        // Tutorial overlay on top of everything
        if (showTutorial) {
            com.platisa.app.ui.screens.help.TutorialOverlay(
                onDismiss = { com.platisa.app.ui.screens.help.TutorialState.dismiss() }
            )
        }
    }
}

