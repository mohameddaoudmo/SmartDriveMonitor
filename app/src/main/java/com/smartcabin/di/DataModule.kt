package com.smartcabin.di

import com.smartcabin.data.source.DataLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindDaemonRepository(
        daemonRepositoryImpl: com.smartcabin.data.repository.DaemonRepositoryImpl
    ): com.smartcabin.domain.repository.DaemonRepository
}
