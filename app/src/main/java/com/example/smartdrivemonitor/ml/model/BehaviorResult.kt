package com.example.smartdrivemonitor.ml.model

data class BehaviorResult(
    val behaviorClass: BehaviorClass,
    val confidence: Float,              // 0.0 - 1.0
    val probabilities: FloatArray,      // Softmax output for the 4 classes
    val inferenceTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isConfident: Boolean get() = confidence >= 0.60f
    val scoreImpact: Int get() = when (behaviorClass) {
        BehaviorClass.NORMAL             ->  0   // Neutral
        BehaviorClass.SUDDEN_ACCELERATION -> -8  // Penalty
        BehaviorClass.HARD_BRAKING        -> -10 // High Penalty
        BehaviorClass.SHARP_TURN          -> -6  // Penalty
        BehaviorClass.UNKNOWN             ->  0  // Neutral
    }
}

enum class BehaviorClass(val displayName: String, val colorHex: String) {
    NORMAL("Normal Driving",          "#10B981"),
    SUDDEN_ACCELERATION("Sudden Acceleration", "#F59E0B"),
    HARD_BRAKING("Hard Braking",     "#EF4444"),
    SHARP_TURN("Sharp Turn",        "#8B5CF6"),
    UNKNOWN("Analyzing...",          "#64748B")
}
