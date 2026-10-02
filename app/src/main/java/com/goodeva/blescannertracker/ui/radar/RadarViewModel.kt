package com.goodeva.blescannertracker.ui.radar

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodeva.blescannertracker.data.ble.BluetoothStateMonitor
import com.goodeva.blescannertracker.domain.RssiSnapshot
import com.goodeva.blescannertracker.domain.RssiTracker
import com.goodeva.blescannertracker.domain.model.BleDevice
import com.goodeva.blescannertracker.domain.BleScanException
import com.goodeva.blescannertracker.domain.BleScanner
import com.goodeva.blescannertracker.domain.model.SignalCategory
import com.goodeva.blescannertracker.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

private const val LOST_TIMEOUT_MS = 8_000L
private const val TICK_MS = 500L

enum class TrackingStatus { SEARCHING, TRACKING, LOST }

enum class Stability(val label: String) {
    UNKNOWN("-"),
    STABLE("Stabil"),
    FAIR("Cukup stabil"),
    UNSTABLE("Tidak stabil"),
}

data class RadarUiState(
    val address: String,
    val name: String? = null,
    val status: TrackingStatus = TrackingStatus.SEARCHING,
    val signal: SignalCategory = SignalCategory.LOST,
    val rawRssi: Int? = null,
    val smoothedRssi: Int? = null,
    val minRssi: Int? = null,
    val maxRssi: Int? = null,
    val packetsInWindow: Int = 0,
    val stability: Stability = Stability.UNKNOWN,
    val secondsSinceLast: Long? = null,
    val bluetoothOn: Boolean = true,
    val errorMessage: String? = null
)

private sealed interface Event {
    data class Packet(val device: BleDevice) : Event
    data class BluetoothState(val on: Boolean) : Event
    data class Failure(val cause: Throwable) : Event
    data object Tick : Event
}

@HiltViewModel
class RadarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scanner: BleScanner,
    private val monitor: BluetoothStateMonitor
) : ViewModel() {

    //Harus dideklarasikan sebelum uiState
    private val address: String = Uri.decode(checkNotNull(savedStateHandle.get<String>(Routes.ARG_ADDRESS)))

    val uiState: StateFlow<RadarUiState> = flow {
        val tracker = RssiTracker()
        val startedAt = SystemClock.elapsedRealtime()
        var name: String? = null
        var bluetoothOn = monitor.isEnabledNow
        var error: String? = null

        emit(RadarUiState(
            address = address,
            bluetoothOn = bluetoothOn
        ))

        // Semua event dalam satu coroutine ini sehingga tracker aman tanpa lock
        merge(trackingEvents(), ticks()).collect { event ->
            val now = SystemClock.elapsedRealtime()
            when (event) {
                is Event.Packet -> {
                    tracker.add(event.device.rssi, now)
                    name = event.device.name ?: name
                }
                is Event.BluetoothState -> {
                    bluetoothOn = event.on
                    if (event.on) error = null
                }
                is Event.Failure -> error = event.cause.toRadarMessage()
                Event.Tick -> Unit

            }
            emit(
                buildState(
                    address = address,
                    name = name,
                    snapshot = tracker.snapshot(now),
                    now = now,
                    startedAt = startedAt,
                    bluetoothOn = bluetoothOn,
                    error = error,
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RadarUiState(address = address),
    )

    private fun buildState(
        address: String,
        name: String?,
        snapshot: RssiSnapshot,
        now: Long,
        startedAt: Long,
        bluetoothOn: Boolean,
        error: String?
    ): RadarUiState {
        val reference = snapshot.lastPacketAtMs ?: startedAt
        val timedOut = now - reference > LOST_TIMEOUT_MS
        val smoothed = snapshot.smoothed?.roundToInt()

        val category = if (timedOut || smoothed == null) SignalCategory.LOST
        else SignalCategory.fromRssi(smoothed)

        val status = when {
            timedOut -> TrackingStatus.LOST
            smoothed == null -> TrackingStatus.SEARCHING
            category == SignalCategory.LOST -> TrackingStatus.LOST
            else -> TrackingStatus.TRACKING
        }

        val stability = when {
            snapshot.packetCount < 3 -> Stability.UNKNOWN
            snapshot.stdDev < 3f -> Stability.STABLE
            snapshot.stdDev < 6f -> Stability.FAIR
            else -> Stability.UNSTABLE
        }

        return RadarUiState(
            address = address,
            name = name,
            status = status,
            signal = category,
            rawRssi = if (timedOut) null else snapshot.raw,
            smoothedRssi = if (timedOut) null else smoothed,
            minRssi = snapshot.min,
            maxRssi = snapshot.max,
            packetsInWindow = snapshot.packetCount,
            stability = stability,
            secondsSinceLast = snapshot.lastPacketAtMs?.let { (now - it) / 1000 },
            bluetoothOn = bluetoothOn,
            errorMessage = error,
        )
    }

    /*
    * Bluetooth ON => Scan
    * OFF => Stop
    * ON => Scan ulang otomatis
    * */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun trackingEvents(): Flow<Event> = monitor.isEnabled.flatMapLatest { on ->
        flow<Event> {
            emit(Event.BluetoothState(on))
            if (on) {
                emitAll(
                    scanner.scan()
                        .filter { it.address == address }
                        .map<BleDevice, Event> { Event.Packet(it) }
                        .catch { emit(Event.Failure(it)) }
                )
            }
        }
     }

    /*
    * Ticks tiap 0,5 detik "x detik lalu" dan
    * Deteksi Lost tetap jalan meski tidak ada paket
    */

    private fun ticks(): Flow<Event> = flow {
        while (true) {
            emit(Event.Tick)
            delay(TICK_MS)
        }
    }

    private fun Throwable.toRadarMessage(): String = when (this) {
        is BleScanException -> when (code) {
            BleScanException.BLUETOOTH_OFF -> "Bluetooth tidak aktif."
            else -> "Pelacakan gagal (kode $code). Coba matikan lalu nyalakan Bluetooth."
        }
        is SecurityException -> "Izin Bluetooth dicabut. Berikan izin kembali."
        else -> "Terjadi kesalahan: ${message ?: "tidak diketahui"}"
    }
}