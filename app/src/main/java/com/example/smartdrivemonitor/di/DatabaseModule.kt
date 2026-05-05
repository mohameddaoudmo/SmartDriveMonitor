package com.example.smartdrivemonitor.di

import android.content.Context
import androidx.room.Room
import com.example.smartdrivemonitor.data.source.db.SmartDriveDatabase
import com.example.smartdrivemonitor.data.source.db.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSmartDriveDatabase(@ApplicationContext context: Context): SmartDriveDatabase {
        return Room.databaseBuilder(
            context,
            SmartDriveDatabase::class.java,
            SmartDriveDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideTripDao(database: SmartDriveDatabase): TripDao {
        return database.tripDao
    }
}
