package com.goodeva.blescannertracker.data.ble.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val address: String,
    val name: String?,
    val lastRssi: Int,
    val lastSeen: Long,
)