package com.platisa.app.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.platisa.app.R
import com.platisa.app.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val splashState by viewModel.splashState.collectAsState()

    LaunchedEffect(splashState) {
        when (splashState) {
            is SplashState.Loading -> {
                // Stay on splash
            }
            is SplashState.NavigateToHome -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is SplashState.NavigateToLogin -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is SplashState.NavigateToOnboarding -> {
                // Logged in but interrupted - go to ScanTimeframe (Onboarding)
                navController.navigate(Screen.ScanTimeframe.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    val splashImageRes = when (viewModel.splashScreenStyle) {
        "LIGHT" -> {
             // Adaptive Default: Check system theme
             if (isDarkTheme) R.drawable.splash_background else R.drawable.platisa_greetings_image_light
        }
        "SPLASH_1" -> R.drawable.splash_option_1
        "SPLASH_2" -> R.drawable.splash_option_2
        "SPLASH_3" -> R.drawable.splash_option_3
        "SPLASH_4" -> R.drawable.splash_option_4
        else -> {
             if (isDarkTheme) R.drawable.splash_background else R.drawable.platisa_greetings_image_light
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = splashImageRes),
            contentDescription = "Splash Screen Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

