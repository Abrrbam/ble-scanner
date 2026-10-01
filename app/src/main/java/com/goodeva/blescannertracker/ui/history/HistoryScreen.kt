package com.goodeva.blescannertracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodeva.blescannertracker.domain.model.BleDevice
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val lastSeenFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale.forLanguageTag("id-ID"))

private fun formatLastSeen(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(lastSeenFormatter)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History", style = MaterialTheme.typography.headlineMedium)
            Row {
                if (devices.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) { Text("Hapus") }
                }
                TextButton(onClick = onBack) { Text("Kembali") }
            }
        }

        if (devices.isEmpty()) {
            Text(
                "Belum ada riwayat. Perangkat yang terdeteksi saat scan akan tersimpan otomatis.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.address }) { HistoryItem(it) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus semua riwayat?") },
            text = { Text("Seluruh riwayat perangkat akan dihapus dan tidak bisa dikembalikan.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun HistoryItem(device: BleDevice) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(device.name ?: "Unknown device", style = MaterialTheme.typography.titleMedium)
            Text(device.address, style = MaterialTheme.typography.bodySmall)
            Text(
                "Terakhir terlihat: ${formatLastSeen(device.lastSeen)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Sinyal terakhir: ${device.rssi} dBm · ${device.signal.label}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}