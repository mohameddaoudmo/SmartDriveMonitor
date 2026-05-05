package com.example.smartdrivemonitor.domain.model

enum class DrivingState {
    NORMAL,              // Safe, normal driving
    HARD_BRAKING,        // Sudden or hard braking
    RAPID_ACCELERATION,  // Sudden acceleration
    SHARP_TURN           // Dangerous turn at high speed
}
