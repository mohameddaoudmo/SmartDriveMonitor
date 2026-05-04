package com.example.smartdrivemonitor.domain.usecase

import com.example.smartdrivemonitor.domain.model.DrivingState
import com.example.smartdrivemonitor.domain.model.SensorFrame
import kotlin.math.abs

class AnalyzeDrivingBehaviorUseCase {

    // استخدام operator fun invoke بيخلينا ننادي الـ UseCase كأنه دالة مباشرة
    operator fun invoke(frames: List<SensorFrame>): DrivingState {
        if (frames.size < 2) return DrivingState.NORMAL

        val firstFrame = frames.first()
        val lastFrame = frames.last()
        
        val timeDiffSeconds = (lastFrame.timestamp - firstFrame.timestamp) / 1000f
        if (timeDiffSeconds <= 0f) return DrivingState.NORMAL

        val speedDiff = lastFrame.speed - firstFrame.speed
        val acceleration = speedDiff / timeDiffSeconds

        val maxBrake = frames.maxOf { it.brake }
        val maxSteering = frames.maxOf { abs(it.steeringAngle) }

        return when {
            acceleration < -5.0f || maxBrake > 80f -> DrivingState.HARD_BRAKING
            acceleration > 5.0f -> DrivingState.RAPID_ACCELERATION
            maxSteering > 45f && lastFrame.speed > 10f -> DrivingState.SHARP_TURN
            else -> DrivingState.NORMAL
        }
    }
}
