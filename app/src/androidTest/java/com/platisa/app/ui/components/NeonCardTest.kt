package com.platisa.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.platisa.app.ui.theme.PlatisaTheme
import com.platisa.app.ui.theme.NeonCyanHome
import org.junit.Rule
import org.junit.Test

/**
 * UI testovi za NeonCard i GlowingCard komponente.
 * 
 * Testira:
 * - Prikaz sadržaja unutar kartice
 * - Stilizacija i modifikatori
 */
class NeonCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun neonCard_displaysContent() {
        composeTestRule.setContent {
            PlatisaTheme {
                NeonCard {
                    Text(text = "Test sadržaj")
                }
            }
        }

        composeTestRule
            .onNodeWithText("Test sadržaj")
            .assertIsDisplayed()
    }

    @Test
    fun neonCard_appliesModifier() {
        composeTestRule.setContent {
            PlatisaTheme {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Text(text = "Kartica sa modifikatorom")
                }
            }
        }

        composeTestRule
            .onNodeWithText("Kartica sa modifikatorom")
            .assertIsDisplayed()
    }

    @Test
    fun glowingCard_displaysContent() {
        composeTestRule.setContent {
            PlatisaTheme {
                GlowingCard(glowColor = NeonCyanHome) {
                    Text(text = "Svetleća kartica")
                }
            }
        }

        composeTestRule
            .onNodeWithText("Svetleća kartica")
            .assertIsDisplayed()
    }

    @Test
    fun glowingCard_withCustomGlowColor() {
        composeTestRule.setContent {
            PlatisaTheme {
                GlowingCard(glowColor = Color.Magenta) {
                    Text(text = "Magenta sjaj")
                }
            }
        }

        composeTestRule
            .onNodeWithText("Magenta sjaj")
            .assertIsDisplayed()
    }

    @Test
    fun neonCard_displaysMultipleChildren() {
        composeTestRule.setContent {
            PlatisaTheme {
                NeonCard {
                    Text(text = "Prvi red")
                    Text(text = "Drugi red")
                }
            }
        }

        composeTestRule
            .onNodeWithText("Prvi red")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Drugi red")
            .assertIsDisplayed()
    }
}

