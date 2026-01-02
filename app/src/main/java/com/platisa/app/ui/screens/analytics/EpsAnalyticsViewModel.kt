package com.platisa.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.model.EpsMonthData
import com.platisa.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EpsAnalyticsViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    val monthlyData: StateFlow<List<EpsMonthData>> = repository.getEpsAnalyticsData()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    init {
        android.util.Log.d("EpsAnalyticsViewModel", "ViewModel initialized")
    }
}

