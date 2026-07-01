package com.smartcabin.domain.usecase

import com.smartcabin.domain.model.DrivingState
import com.smartcabin.domain.model.SensorFrame
import javax.inject.Inject
import kotlin.math.abs

class AnalyzeDrivingBehaviorUseCase @Inject constructor() {

    /**
     * Thresholds for rule-based behavioral labeling.
     * Note: This version is tuned for emulator data where some sensors (like Brake) are derived.
     */
    operator fun invoke(frame: SensorFrame): DrivingState {
        val speedKmh = frame.speed * 3.6f

        // Sudden Acceleration: High Speed combined with High RPM
        if (frame.rpm > 3500f && speedKmh > 40f) {
            return DrivingState.SUDDEN_ACCELERATION
        }
        
        // Hard Braking: High derived brake force
        if (frame.brake > 0.4f) {
            return DrivingState.HARD_BRAKING
        }
        
        // Sharp Turn: Significant steering angle
        if (abs(frame.steeringAngle) > 25f) {
            return DrivingState.SHARP_TURN
        }
        
        return DrivingState.NORMAL
    }
}
