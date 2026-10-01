package com.goodeva.blescannertracker.data.ble.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    abstract fun observeAll(): Flow<List<DeviceEntity>>

    //Yg baru, masuk. Yg udah ada diabaikan (nanti diupdate update() di bawah)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(entity: DeviceEntity): Long

    //COALESCE: Kalau nama baru null, pertahankan nama lama
    @Query(
        "UPDATE devices SET name = COALESCE(:name, name), lastRssi = :rssi, lastSeen = :lastSeen " +
        "WHERE address = :address",
    )
    abstract suspend fun update(address: String, name: String?, rssi: Int, lastSeen: Long)

    @Transaction
    open suspend fun saveAll(devices: List<DeviceEntity>) {
        devices.forEach {
            insertIgnore(it)
            update(it.address, it.name, it.lastRssi, it.lastSeen)
        }
    }

    @Query("DELETE FROM devices")
    abstract suspend fun clear()

}