package com.smartcabin.di

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import com.smartcabin.data.repository.VhalRepositoryImpl
import com.smartcabin.data.source.DataLogger
import com.smartcabin.data.source.VhalDataSource
import com.smartcabin.domain.repository.VhalRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindVhalRepository(
        vhalRepositoryImpl: VhalRepositoryImpl
    ): VhalRepository

    @Binds
    @Singleton
    abstract fun bindDaemonRepository(
        daemonRepositoryImpl: com.smartcabin.data.repository.DaemonRepositoryImpl
    ): com.smartcabin.domain.repository.DaemonRepository

    companion object {
        @Provides
        @Singleton
        fun provideCarPropertyManager(@ApplicationContext context: Context): CarPropertyManager {
            val car = Car.createCar(context)
            return car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        }
    }
}
