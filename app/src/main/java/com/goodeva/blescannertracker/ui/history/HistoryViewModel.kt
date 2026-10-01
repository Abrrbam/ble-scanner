package com.goodeva.blescannertracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodeva.blescannertracker.domain.DeviceRepository
import com.goodeva.blescannertracker.domain.model.BleDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {
    val devices: StateFlow<List<BleDevice>> = repository.observerHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }
}