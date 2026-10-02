package com.goodeva.blescannertracker.data.repository

import com.goodeva.blescannertracker.data.local.DeviceDao
import com.goodeva.blescannertracker.data.local.DeviceEntity
import com.goodeva.blescannertracker.domain.DeviceRepository
import com.goodeva.blescannertracker.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDeviceRepository @Inject constructor(
    private val dao: DeviceDao
) : DeviceRepository {

    override fun observerHistory(): Flow<List<BleDevice>> =
        dao.observeAll().map { list -> list.map { it.toDomain() }}

    override suspend fun save(devices: List<BleDevice>) =
        dao.saveAll(devices.map { it.toEntity() })

    override suspend fun clear() = dao.clear()
}

private fun DeviceEntity.toDomain() = BleDevice(
    address = address,
    name = name,
    rssi = lastRssi,
    lastSeen = lastSeen
)

private fun BleDevice.toEntity() = DeviceEntity(
    address = address,
    name = name,
    lastRssi = rssi,
    lastSeen = lastSeen
)

