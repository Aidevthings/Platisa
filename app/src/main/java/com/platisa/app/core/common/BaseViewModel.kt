package com.platisa.app.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val handler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "Coroutine exception caught in BaseViewModel")
        handleError(exception)
    }

    protected fun launchCatching(
        showLoading: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(handler) {
            if (showLoading) _isLoading.value = true
            try {
                block()
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    open fun handleError(exception: Throwable) {
        viewModelScope.launch {
            _error.emit(exception.message ?: "Unknown error occurred")
        }
    }
}

