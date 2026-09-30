package com.goodeva.blescannertracker.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScannerScreen(
    onDeviceClick: (String) -> Unit,
    onHistoryClick: () -> Unit
){
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Scanner",
            style = MaterialTheme.typography.headlineMedium
        )
        Button(onClick = { onDeviceClick("AA:BB:CC:DD:EE:FF") }) { Text("Device → Radar") }
        Button(onClick = onHistoryClick) { Text("History") }
    }

}