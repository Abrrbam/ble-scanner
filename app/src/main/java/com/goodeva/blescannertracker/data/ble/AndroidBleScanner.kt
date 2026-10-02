package com.goodeva.blescannertracker.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.goodeva.blescannertracker.domain.model.BleDevice
import com.goodeva.blescannertracker.domain.BleScanException
import com.goodeva.blescannertracker.domain.BleScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBleScanner @Inject constructor(
    @ApplicationContext context: Context,
) : BleScanner {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter

    //Permission dicek di UI saat sebelum scan. SecurityExc tetap ditangani
    @SuppressLint("MissingPermission")
    override fun scan(): Flow<BleDevice> = callbackFlow {
        //Bluetooth mati = BLEScanner null
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            close(BleScanException(BleScanException.BLUETOOTH_OFF))
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
//                Log.d("BleScan", "${result.device.address} rssi=${result.rssi}")
                trySend(result.toDevice())
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { trySend(it.toDevice()) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(BleScanException(errorCode))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, callback)
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (_: SecurityException) {
            } catch (_: IllegalStateException){
            }
        }
    }

    private fun ScanResult.toDevice() = BleDevice(
        address = device.address,
        name = scanRecord?.deviceName, //tidak butuh BLUETOOTH_CONNECT
        rssi = rssi,
        lastSeen = System.currentTimeMillis(),

    )
}