package com.smartcabin.ml

import com.smartcabin.domain.model.SensorFrame
import kotlin.math.abs
import kotlin.math.max

class FeatureExtractor {

    private var prevSpeedMs: Float = 0f
    private var prevRpm: Float     = 0f
    private var prevSpeedDelta: Float = 0f

    /**
     * Extracts 11 behavioral features from a raw SensorFrame.
     * The order of features MUST match the training pipeline's feature list.
     */
    fun extract(frame: SensorFrame): FloatArray {
        val speedDelta   = frame.speed - prevSpeedMs      // Acceleration/Deceleration (m/s per sample)
        val rpmDelta     = frame.rpm - prevRpm              // RPM change
        val absSteer     = abs(frame.steeringAngle)         // Steering intensity
        val jerk         = speedDelta - prevSpeedDelta      // Rate of acceleration change
        val brakeXSpeed  = frame.brake * frame.speed        // Effective braking force
        val rpmPerSpeed  = frame.rpm / max(frame.speed, 0.1f) // Engine load efficiency
        val gearNorm     = frame.gear / 16.0f               // Normalized gear index

        // Update state for temporal features
        prevSpeedDelta = speedDelta
        prevSpeedMs    = frame.speed
        prevRpm        = frame.rpm

        return floatArrayOf(
            frame.speed,      // 0: speed_ms
            frame.rpm,          // 1: rpm
            frame.steeringAngle,// 2: steering_angle
            frame.brake,   // 3: brake_input
            speedDelta,         // 4: speed_delta
            rpmDelta,           // 5: rpm_delta
            absSteer,           // 6: abs_steering
            jerk,               // 7: jerk
            brakeXSpeed,        // 8: brake_x_speed
            rpmPerSpeed,        // 9: rpm_per_speed
            gearNorm            // 10: gear_norm
        )
    }

    fun reset() {
        prevSpeedMs    = 0f
        prevRpm        = 0f
        prevSpeedDelta = 0f
    }
}
