package com.goodeva.blescannertracker.domain

import com.goodeva.blescannertracker.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observerHistory(): Flow<List<BleDevice>>
    suspend fun save(devices: List<BleDevice>)
    suspend fun clear()
}