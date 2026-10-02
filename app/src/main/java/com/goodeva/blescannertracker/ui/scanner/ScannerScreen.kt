package com.goodeva.blescannertracker.ui.scanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodeva.blescannertracker.domain.DeviceFilter
import com.goodeva.blescannertracker.domain.RSSI_FILTER_OFF
import com.goodeva.blescannertracker.domain.model.BleDevice
import com.goodeva.blescannertracker.ui.util.findActivity
import com.goodeva.blescannertracker.ui.util.hasBleScanPermission
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

//
private const val STALE_AFTER_SEC = 10L

@Composable
fun ScannerScreen(
    onDeviceClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
){
    val context = LocalContext.current
    val bluetoothOn by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    // Hentikan scan saat App ke background agar menghemat baterai dan mengikuti "App Lifecycle"
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val isRotating = context.findActivity()?.isChangingConfigurations == true
        if (!isRotating) viewModel.stopScan()
    }

    var hasPermission by remember { mutableStateOf(context.hasBleScanPermission()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false)  }

    // Cek ulang permission karna bisa saja diubah lewat Settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = context.hasBleScanPermission()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted
        if (!granted) {
            //Jika ditolak, maka akan diset false sehingga sistem tidak brtanya lagi
            permanentlyDenied = context.findActivity()?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    it, Manifest.permission.BLUETOOTH_SCAN
                )
            } ?: false
        }

    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Scanner",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = onHistoryClick) { Text("History") }

        }

        when {
            !viewModel.isBluetoothSupported -> StatusMessage(
                title = "Bluetooth tidak tersedia",
                message = "Perangkat ini tidak mendukung Bluetooth.",
            )

            !hasPermission && permanentlyDenied -> StatusMessage(
                title = "Izin diperlukan",
                message = "Izin \"Perangkat di sekitar\" ditolak. Aktifkan lewat pengaturan aplikasi.",
                actionLabel = "Buka Pengaturan",
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
            )

            !hasPermission -> StatusMessage(
                title = "Izin diperlukan",
                message = "Aplikasi butuh izin \"Perangkat di sekitar\" untuk memindai perangkat BLE.",
                actionLabel = "Beri Izin",
                onAction = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN) },
            )

            !bluetoothOn -> StatusMessage(
                title = "Bluetooth mati",
                message = "Nyalakan Bluetooth untuk mulai memindai.",
                actionLabel = "Buka Pengaturan Bluetooth",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
            )

            else -> ScanContent(
                state = uiState,
                filter = filter,
                onStart = viewModel::startScan,
                onStop = viewModel::stopScan,
                onDeviceClick = onDeviceClick,
                onDismissError = viewModel::dismissError,
                onQueryChange = viewModel::onQueryChange,
                onMinRssiChange = viewModel::onMinRssiChange
            )
        }
    }
}

@Composable
private fun ScanContent(
    state: ScannerUiState,
    filter: DeviceFilter,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onDismissError: () -> Unit,
    onQueryChange: (String) -> Unit,
    onMinRssiChange: (Int) -> Unit,
) {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1_000.milliseconds)
            value = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val counter = "${state.devices.size}/${state.totalCount}"
            Text(
                if (state.isScanning) "Memindai... ($counter)"
                else "Berhenti ($counter)"
            )
            if (state.isScanning) {
                OutlinedButton(onClick = onStop) { Text("Stop") }
            } else {
                Button(onClick = onStart) { Text("Start") }
            }
        }

        OutlinedTextField(
            value = filter.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Cari nama atau MAC") },
            trailingIcon = {
                if (filter.query.isNotEmpty()) {
                    TextButton(onClick = { onQueryChange("") }) { Text("Hapus") }
                }
            },
        )

        Column {
            Text(
                text = if (filter.minRssi <= RSSI_FILTER_OFF) "Filter sinyal: semua"
                else "Filter sinyal: ≥ ${filter.minRssi} dBm",
                style = MaterialTheme.typography.bodySmall,
            )
            Slider(
                value = filter.minRssi.toFloat(),
                onValueChange = { onMinRssiChange(it.roundToInt()) },
                valueRange = -100f..-30f,
                steps = 13, // kelipatan 5 dBm
            )
        }

        state.errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(message, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissError) { Text("Tutup") }
                }
            }
        }

        when {
            state.totalCount == 0 -> Text(
                if (state.isScanning) "Mencari perangkat…" else "Tekan Start untuk mulai memindai.",
                style = MaterialTheme.typography.bodyMedium,
            )

            state.devices.isEmpty() -> Text(
                "Tidak ada perangkat yang cocok dengan filter.",
                style = MaterialTheme.typography.bodyMedium,
            )

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.devices, key = { it.address }) { device ->
                    DeviceItem(
                        device = device,
                        now = now,
                        onClick = { onDeviceClick(device.address) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
    @Composable
    private fun DeviceItem(
        device: BleDevice,
        now: Long,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {

        val ageSec = ((now - device.lastSeen) / 1000).coerceAtLeast(0)
        val stale = ageSec >= STALE_AFTER_SEC

        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().alpha(if (stale) 0.5f else 1f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device.name ?: "Unknown device",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(device.address, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${device.rssi} dBm", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (stale) "Tidak terlihat $ageSec dtk"
                        else "${device.signal.label} · ${device.signal.distance}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusMessage(
        title: String,
        message: String,
        actionLabel: String? = null,
        onAction: () -> Unit = {},
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            if (actionLabel != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
