package com.goodeva.blescannertracker.domain.model

import kotlinx.coroutines.flow.Flow

interface BleScanner {
    /** Cold flow: scan mulai saat di-collect, berhenti saat collector dibatalkan. */
    fun scan(): Flow<BleDevice>
}

class BleScanException(val code: Int) : Exception("BLE scan failed, code =$code") {
    companion object {
        const val BLUETOOTH_OFF = -1
        const val SCANNING_TOO_FREQUENTLY = 6
    }
}