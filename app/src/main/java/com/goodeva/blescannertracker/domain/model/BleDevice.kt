package com.goodeva.blescannertracker.domain.model

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeen: Long,
) {
    val signal: SignalCategory get() = SignalCategory.fromRssi(rssi)

}