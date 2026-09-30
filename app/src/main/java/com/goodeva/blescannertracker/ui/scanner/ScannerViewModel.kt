package com.goodeva.blescannertracker.ui.scanner

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodeva.blescannertracker.data.ble.BluetoothStateMonitor
import com.goodeva.blescannertracker.domain.model.BleDevice
import com.goodeva.blescannertracker.domain.model.BleScanException
import com.goodeva.blescannertracker.domain.model.BleScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Batas Android: maksimal 5 kali start scan dalam 30 detik
private const val SCAN_WINDOW_MS = 30_000L
private const val MAX_STARTS_PER_WINDOW = 5

data class ScannerUiState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    monitor: BluetoothStateMonitor,
    private val scanner: BleScanner
) : ViewModel() {
    val isBluetoothSupported: Boolean = monitor.isSupported

    val isBluetoothEnabled: StateFlow<Boolean> = monitor.isEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = monitor.isEnabledNow,
    )

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    // LinkedHashMap: update perangkat yang sama tidak mengubah posisinya di daftar
    private val deviceMap = LinkedHashMap<String, BleDevice>()
    private val startTimestamps = ArrayDeque<Long>()

    init {
        //Hentikan scan ketika Bluetooth dimatikan ditengah scan
        viewModelScope.launch {
            monitor.isEnabled.filter { !it }.collect { stopScan() }
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return

        val now = SystemClock.elapsedRealtime()
        while (startTimestamps.isNotEmpty() && now - startTimestamps.first() > SCAN_WINDOW_MS) {
            startTimestamps.removeFirst()
        }
        if (startTimestamps.size >= MAX_STARTS_PER_WINDOW) {
            val waitSeconds = (SCAN_WINDOW_MS - (now - startTimestamps.first())) / 1000 + 1
            _uiState.update {
                it.copy(errorMessage = "Scan dimulai terlalu sering. Tunggu $waitSeconds detik lalu coba lagi.")
            }
            return
        }
        startTimestamps.addLast(now)

        deviceMap.clear()

        _uiState.update { ScannerUiState(isScanning = true) }
        scanJob = viewModelScope.launch {
            scanner.scan()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.toUserMessage()) } }
                .onCompletion { _uiState.update { it.copy(isScanning = false) } }
                .collect { device ->
                    deviceMap[device.address] = device
                    _uiState.update { it.copy(devices = deviceMap.values.toList())
                    }
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    private fun Throwable.toUserMessage(): String = when (this) {
        is BleScanException -> when (code) {
            BleScanException.BLUETOOTH_OFF -> "Bluetooth tidak aktif."
            BleScanException.SCANNING_TOO_FREQUENTLY ->
                "Scan dimulai terlalu sering. Tunggu sekitar 30 detik, lalu coba lagi."
            else -> "Scan gagal (kode $code). Coba matikan lalu nyalakan Bluetooth."
        }
        is SecurityException -> "Izin Bluetooth dicabut. Berikan izin kembali."
        else -> "Terjadi kesalahan: ${message ?: "tidak diketahui"}"
    }
}