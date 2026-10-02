package com.goodeva.blescannertracker.ui.radar

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RadarScreen(
    onBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val signalColor = state.signal.toColor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Radar", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onBack) { Text("Kembali") }
        }

        Column {
            Text(state.name ?: "Unknown device", style = MaterialTheme.typography.titleLarge)
            Text(state.address, style = MaterialTheme.typography.bodySmall)
        }

        if (!state.bluetoothOn) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Bluetooth mati. Pelacakan berlanjut otomatis setelah Bluetooth menyala.")
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }) { Text("Buka Pengaturan Bluetooth") }
                }
            }
        }

        state.errorMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(message, modifier = Modifier.padding(12.dp))
            }
        }

        RadarView(
            signal = state.signal,
            smoothedRssi = state.smoothedRssi,
            status = state.status,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.status) {
                TrackingStatus.SEARCHING -> Text(
                    "Mencari perangkat…",
                    style = MaterialTheme.typography.titleMedium,
                )

                TrackingStatus.LOST -> {
                    Text(
                        "Sinyal hilang",
                        style = MaterialTheme.typography.headlineMedium,
                        color = signalColor,
                    )
                    Text("Terputus / di luar jangkauan", style = MaterialTheme.typography.bodyMedium)
                }

                TrackingStatus.TRACKING -> {
                    Text(
                        "${state.smoothedRssi} dBm",
                        style = MaterialTheme.typography.headlineLarge,
                        color = signalColor,
                    )
                    Text(
                        "${state.signal.label} · ${state.signal.distance}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DetailRow("RSSI mentah", state.rawRssi.asDbm())
                DetailRow("RSSI halus (rata-rata)", state.smoothedRssi.asDbm())
                DetailRow(
                    "Min / Maks (10 dtk)",
                    if (state.minRssi != null && state.maxRssi != null)
                        "${state.minRssi} / ${state.maxRssi} dBm" else "—",
                )
                DetailRow("Paket diterima (10 dtk)", state.packetsInWindow.toString())
                DetailRow("Stabilitas sinyal", state.stability.label)
                DetailRow(
                    "Terakhir diterima",
                    when (val s = state.secondsSinceLast) {
                        null -> "—"
                        0L -> "baru saja"
                        else -> "$s detik lalu"
                    },
                )
            }
        }

        Text(
            "Catatan: BLE hanya memberi perkiraan jarak dari kekuatan sinyal, bukan arah. " +
                    "Posisi titik di radar menunjukkan jarak, bukan arah sebenarnya.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun Int?.asDbm(): String = this?.let { "$it dBm" } ?: "—"

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}