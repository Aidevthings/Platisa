package com.example.platisa.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.platisa.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prošireni UI testovi za navigaciju u aplikaciji.
 * 
 * Testira:
 * - Prikaz početnog ekrana (Login)
 * - Navigacione elemente
 * - Osnovne korisničke tokove
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_startsWithLoginScreen() {
        // Aplikacija treba da počne sa Login ekranom
        composeTestRule
            .onNodeWithText("Platiša")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_hasGoogleSignInButton() {
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun loginScreen_displaysAppBranding() {
        // Proveri da se prikazuje brending
        composeTestRule
            .onNodeWithText("Platiša")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Vaš pametni asistent za račune")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysConnectGmailCard() {
        composeTestRule
            .onNodeWithText("Povežite Gmail nalog")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_hasProperLayout() {
        // Proveri da svi glavni elementi postoje
        composeTestRule.onNodeWithText("Platiša").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prijavi se sa Google-om").assertIsDisplayed()
        
        // Čekaj da se UI stabilizuje
        composeTestRule.waitForIdle()
    }

    // ============================================
    // DODATNI TESTOVI ZA VERIFIKACIJU STANJA
    // ============================================

    @Test
    fun app_initialState_isNotLoggedIn() {
        // Na početku korisnik nije ulogovan, tako da se prikazuje Login ekran
        composeTestRule
            .onNodeWithText("Povežite Gmail nalog")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_buttonIsEnabled() {
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .assertIsEnabled()
    }
}
