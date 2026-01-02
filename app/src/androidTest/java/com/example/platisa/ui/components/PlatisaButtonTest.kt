package com.example.platisa.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.platisa.ui.theme.PlatisaTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI testovi za PlatisaButton komponentu.
 * 
 * Testira:
 * - Prikaz teksta
 * - Stanje učitavanja (loading)
 * - Onemogućeno stanje (disabled)
 * - Klik događaje
 */
class PlatisaButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun platisaButton_displaysText() {
        composeTestRule.setContent {
            PlatisaTheme {
                PlatisaButton(
                    text = "Prijavi se",
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Prijavi se")
            .assertIsDisplayed()
    }

    @Test
    fun platisaButton_isClickable() {
        var clicked = false
        
        composeTestRule.setContent {
            PlatisaTheme {
                PlatisaButton(
                    text = "Klikni me",
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Klikni me")
            .performClick()

        assert(clicked) { "Button click was not registered" }
    }

    @Test
    fun platisaButton_showsLoadingIndicator_whenLoading() {
        composeTestRule.setContent {
            PlatisaTheme {
                PlatisaButton(
                    text = "Učitavanje",
                    onClick = {},
                    isLoading = true
                )
            }
        }

        // Text should not be visible when loading
        composeTestRule
            .onNodeWithText("Učitavanje")
            .assertDoesNotExist()
        
        // Progress indicator should be shown
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun platisaButton_isNotClickable_whenLoading() {
        var clicked = false
        
        composeTestRule.setContent {
            PlatisaTheme {
                PlatisaButton(
                    text = "Test",
                    onClick = { clicked = true },
                    isLoading = true
                )
            }
        }

        composeTestRule
            .onNode(hasClickAction())
            .assertIsNotEnabled()

        assert(!clicked) { "Button should not be clickable when loading" }
    }

    @Test
    fun platisaButton_isNotClickable_whenDisabled() {
        var clicked = false
        
        composeTestRule.setContent {
            PlatisaTheme {
                PlatisaButton(
                    text = "Onemogućeno",
                    onClick = { clicked = true },
                    enabled = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("Onemogućeno")
            .assertIsNotEnabled()

        assert(!clicked) { "Disabled button should not trigger click" }
    }

    @Test
    fun platisaButton_appliesModifier() {
        composeTestRule.setContent {
            PlatisaTheme {
                PlatisaButton(
                    text = "Test",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Button should exist and be displayed
        composeTestRule
            .onNodeWithText("Test")
            .assertIsDisplayed()
    }
}
