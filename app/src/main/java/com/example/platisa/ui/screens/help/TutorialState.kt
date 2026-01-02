package com.example.platisa.ui.screens.help

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared state object for coordinating tutorial display across screens.
 * This allows the tutorial overlay to be rendered at MainScreen level
 * (giving it access to nav bar icons) while triggers can be in HomeScreen.
 */
object TutorialState {
    private val _showTutorial = MutableStateFlow(false)
    val showTutorial = _showTutorial.asStateFlow()
    
    fun show() {
        _showTutorial.value = true
    }
    
    fun dismiss() {
        _showTutorial.value = false
    }
}
