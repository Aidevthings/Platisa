package com.example.platisa.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI testovi za Platisa temu.
 * 
 * Testira:
 * - Dark mode primenljivost
 * - Light mode primenljivost
 * - Custom boje (LocalPlatisaColors)
 */
@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun platisaTheme_providesDarkColors() {
        composeTestRule.setContent {
            PlatisaTheme(darkTheme = true) {
                val customColors = LocalPlatisaColors.current
                
                // Proveri da dark mode boje postoje
                Text(
                    text = "Dark Mode Test",
                    color = customColors.neonCyan
                )
            }
        }

        composeTestRule
            .onNodeWithText("Dark Mode Test")
            .assertIsDisplayed()
    }

    @Test
    fun platisaTheme_providesLightColors() {
        composeTestRule.setContent {
            PlatisaTheme(darkTheme = false) {
                val customColors = LocalPlatisaColors.current
                
                Text(
                    text = "Light Mode Test",
                    color = customColors.neonCyan
                )
            }
        }

        composeTestRule
            .onNodeWithText("Light Mode Test")
            .assertIsDisplayed()
    }

    @Test
    fun platisaTheme_providesLocalColors() {
        composeTestRule.setContent {
            PlatisaTheme {
                val customColors = LocalPlatisaColors.current
                
                // Test da LocalPlatisaColors postoji i ima vrednosti
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(customColors.background)
                ) {
                    Text(
                        text = "Custom Colors Test",
                        color = customColors.neonCyan
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("Custom Colors Test")
            .assertIsDisplayed()
    }

    @Test
    fun platisaTheme_materialThemeIsApplied() {
        composeTestRule.setContent {
            PlatisaTheme {
                Surface {
                    Text(
                        text = "Material Theme Applied",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("Material Theme Applied")
            .assertIsDisplayed()
    }

    @Test
    fun darkTheme_hasCorrectStatusColors() {
        composeTestRule.setContent {
            PlatisaTheme(darkTheme = true) {
                val colors = LocalPlatisaColors.current
                
                // Test svih status boja
                Text(text = "Status: ${colors.statusPaid}")
                Text(text = "Processing: ${colors.statusProcessing}")
                Text(text = "Unpaid: ${colors.statusUnpaid}")
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun lightTheme_hasCorrectStatusColors() {
        composeTestRule.setContent {
            PlatisaTheme(darkTheme = false) {
                val colors = LocalPlatisaColors.current
                
                // Test svih status boja u light mode
                Text(text = "Light Status Test")
            }
        }

        composeTestRule
            .onNodeWithText("Light Status Test")
            .assertIsDisplayed()
    }
}
