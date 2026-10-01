package com.goodeva.blescannertracker.di

import com.goodeva.blescannertracker.data.ble.repository.RoomDeviceRepository
import com.goodeva.blescannertracker.domain.DeviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDeviceRepository(impl: RoomDeviceRepository): DeviceRepository
}