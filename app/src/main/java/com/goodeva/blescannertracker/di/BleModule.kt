package com.goodeva.blescannertracker.di

import com.goodeva.blescannertracker.data.ble.AndroidBleScanner
import com.goodeva.blescannertracker.domain.BleScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {
    @Binds
    abstract fun bindBleScanner(impl: AndroidBleScanner): BleScanner
}