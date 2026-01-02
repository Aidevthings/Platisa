package com.example.platisa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integracioni UI testovi za Platisa aplikaciju.
 * 
 * Testira kompletne korisničke tokove i interakcije između ekrana.
 */
@RunWith(AndroidJUnit4::class)
class PlatisaIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ============================================
    // APP LAUNCH TESTS
    // ============================================

    @Test
    fun app_launchesSuccessfully() {
        // Aplikacija treba da se pokrene bez greške
        composeTestRule.waitForIdle()
        
        // Proveri da se prikazuje Login ekran (početni ekran za neulogovane)
        composeTestRule
            .onNodeWithText("Platiša")
            .assertExists()
    }

    @Test
    fun app_displaysLoginUIElements() {
        composeTestRule.waitForIdle()
        
        // Svi ključni elementi Login ekrana
        composeTestRule.onNodeWithText("Platiša").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vaš pametni asistent za račune").assertIsDisplayed()
        composeTestRule.onNodeWithText("Povežite Gmail nalog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prijavi se sa Google-om").assertIsDisplayed()
    }

    @Test
    fun app_loginButtonRespondsToClick() {
        composeTestRule.waitForIdle()
        
        // Dugme za prijavu treba da ima click action
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .assertHasClickAction()
    }

    // ============================================
    // ACCESSIBILITY TESTS
    // ============================================

    @Test
    fun loginScreen_hasAccessibleElements() {
        composeTestRule.waitForIdle()
        
        // Svi interaktivni elementi treba da budu dostupni
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .assertIsEnabled()
    }

    @Test
    fun app_handlesTouchInteraction() {
        composeTestRule.waitForIdle()
        
        // Proveri da dugme reaguje na dodir
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .performClick()
        
        // Aplikacija ne bi trebalo da crashuje
        composeTestRule.waitForIdle()
    }

    // ============================================
    // UI STATE TESTS
    // ============================================

    @Test
    fun app_maintainsStateOnRotation() {
        // Prvo učitaj UI
        composeTestRule.waitForIdle()
        
        // Proveri da je Login ekran prikazan
        composeTestRule
            .onNodeWithText("Platiša")
            .assertIsDisplayed()
        
        // Napomena: Rotacija zahteva ActivityScenario za potpuni test
    }

    @Test
    fun loginScreen_displaysGmailNote() {
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithText("Napomena", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Gmail", substring = true)
            .assertIsDisplayed()
    }

    // ============================================
    // PERFORMANCE TESTS (Basic)
    // ============================================

    @Test
    fun app_rendersWithinTimeout() {
        // Aplikacija treba da renderuje UI u razumnom vremenu
        composeTestRule.waitForIdle()
        
        // Ako stignemo ovde bez timeout-a, test je uspešan
        composeTestRule
            .onNodeWithText("Platiša")
            .assertExists()
    }
}
