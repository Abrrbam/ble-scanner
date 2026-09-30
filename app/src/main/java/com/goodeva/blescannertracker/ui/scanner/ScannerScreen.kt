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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodeva.blescannertracker.ui.util.findActivity
import com.goodeva.blescannertracker.ui.util.hasBleScanPermission

@Composable
fun ScannerScreen(
    onDeviceClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
){
    val context = LocalContext.current
    val bluetoothOn by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()

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

            else -> {
                Text("Siap memindai ✅")
                Button(onClick = { onDeviceClick("AA:BB:CC:DD:EE:FF") }) {
                    Text("Dummy device → Radar")
                }
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