package com.platisa.app.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.platisa.app.ui.navigation.Screen
import com.platisa.app.ui.theme.CyberCyan
import androidx.compose.ui.text.font.FontFamily // Added
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.util.*

private val CyberpunkFontFamily = FontFamily.Monospace // Use Monospace for that "cyber" look

@Composable
fun GreetingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    // Get user name directly from Google Sign-In to avoid blocking on ViewModel
    val userName = remember {
        com.platisa.app.core.common.GoogleAuthManager.getSignedInAccount(context)?.givenName ?: "Korisnik"
    }
    var visible by remember { mutableStateOf(false) }
    
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Dobro jutro"
            in 12..17 -> "Dobar dan"
            in 18..21 -> "Dobro veče"
            else -> "Laku noć"
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        visible = true
        delay(1800) // Show for 1.8 seconds
        visible = false
        delay(600) // Wait for fade out
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Greetings.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(800)) + expandVertically(animationSpec = tween(800)),
            exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(animationSpec = tween(500))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Glow effect back
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = greeting,
                        color = CyberCyan.copy(alpha = 0.3f),
                        fontFamily = CyberpunkFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.blur(12.dp)
                    )
                    Text(
                        text = greeting,
                        color = Color.White,
                        fontFamily = CyberpunkFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userName.uppercase(),
                        color = CyberCyan.copy(alpha = 0.5f),
                        fontFamily = CyberpunkFontFamily,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.blur(16.dp)
                    )
                    Text(
                        text = userName.uppercase(),
                        color = CyberCyan,
                        fontFamily = CyberpunkFontFamily,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

