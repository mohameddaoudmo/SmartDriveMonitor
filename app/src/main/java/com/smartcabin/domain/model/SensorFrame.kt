package com.smartcabin.domain.model

/**
 * Data class representing a snapshot of vehicle sensor data at a specific point in time.
 */
data class SensorFrame(
    val speed: Float = 0f,
    val rpm: Float = 0f,
    val steeringAngle: Float = 0f,
    val brake: Float = 0f,
    val gear: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
