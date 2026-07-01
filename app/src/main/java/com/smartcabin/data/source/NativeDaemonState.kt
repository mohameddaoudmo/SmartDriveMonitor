package com.smartcabin.data.source

class NativeDaemonState(
    val rawSpeed: Float,
    val fusedSpeed: Float,
    val acceleration: Float,
    val brake: Float,
    val steeringAngle: Float,
    val rpm: Float,
    val safetyScore: Float,
    val dominantRuleIndex: Int,
    val ruleText: String,
    val confidence: Float,
    val timestamp: Long
)
