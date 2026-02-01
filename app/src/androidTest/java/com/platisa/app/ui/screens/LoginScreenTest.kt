package com.platisa.app.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.platisa.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI testovi za Login ekran.
 * 
 * Testira:
 * - Prikaz naslova aplikacije
 * - Prikaz dugmeta za prijavu
 * - Prikaz informativnih tekstova
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreen_displaysAppTitle() {
        // Na login ekranu treba da se prikaže naslov "Platiša"
        composeTestRule
            .onNodeWithText("Platiša")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysSubtitle() {
        composeTestRule
            .onNodeWithText("Vaš pametni asistent za račune")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysGmailConnectCard() {
        composeTestRule
            .onNodeWithText("Povežite Gmail nalog")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysLoginButton() {
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysGmailOnlyNote() {
        composeTestRule
            .onNodeWithText("Napomena: Aplikacija podržava isključivo Gmail naloge.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_loginButtonIsClickable() {
        composeTestRule
            .onNodeWithText("Prijavi se sa Google-om")
            .assertHasClickAction()
    }

    @Test
    fun loginScreen_displaysInstructionText() {
        composeTestRule
            .onNodeWithText("Da bismo automatski pronašli vaše račune", substring = true)
            .assertIsDisplayed()
    }
}

