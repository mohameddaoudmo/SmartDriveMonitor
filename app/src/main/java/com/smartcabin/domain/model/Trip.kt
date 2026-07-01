package com.smartcabin.domain.model

data class Trip(
    val id: Int = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Float = 0f,
    val avgSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val finalScore: Int = 100,
    val status: TripStatus = TripStatus.IN_PROGRESS
)

enum class TripStatus {
    IN_PROGRESS,
    COMPLETED
}
