package com.example.smartdrivemonitor.domain.model

// الكلاس ده بيمثل "اللقطة" اللي بناخدها من العربية كل جزء من الثانية
data class SensorFrame(
    val speed: Float = 0f,
    val rpm: Float = 0f,
    val steeringAngle: Float = 0f,
    val brake: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
