package com.example.smartdrivemonitor.data.source.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class SmartDriveDatabase : RoomDatabase() {
    abstract val tripDao: TripDao

    companion object {
        const val DATABASE_NAME = "smart_drive_db"
    }
}
