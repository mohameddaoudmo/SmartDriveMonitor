package com.smartcabin.data.source.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Float,
    val avgSpeed: Float,
    val maxSpeed: Float,
    val finalScore: Int,
    val status: String
)
