package com.example.smartdrivemonitor.ml

import com.example.smartdrivemonitor.domain.model.SensorFrame
import com.example.smartdrivemonitor.ml.model.BehaviorResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorClassifier @Inject constructor(
    private val engine: MLInferenceEngine
) {
    /**
     * Processes a SensorFrame.
     * Returns a BehaviorResult if the sliding window is full and inference is performed.
     */
    fun addFrame(frame: SensorFrame): BehaviorResult? {
        // We pass the raw values. SmartDriveProcessor handles windowing internally.
        // It will return null until the first 50 frames are collected.
        return engine.processAndClassify(
            speedMs = frame.speed,
            rpm = frame.rpm,
            steering = frame.steeringAngle,
            brake = frame.brake,
            gearNorm = 0.5f // Matching the default in user's provided code
        )
    }

    fun reset() {
        // Optional: If we want to add a reset to SmartDriveProcessor later
    }
}
