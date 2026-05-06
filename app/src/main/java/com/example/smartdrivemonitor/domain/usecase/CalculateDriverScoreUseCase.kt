package com.example.smartdrivemonitor.domain.usecase

import com.example.smartdrivemonitor.domain.model.DrivingState

class CalculateDriverScoreUseCase {

    // Initial driver score is set to 100
    private var currentScore = 100.0

    operator fun invoke(state: DrivingState): Int {
        val penalty = when (state) {
            DrivingState.HARD_BRAKING -> 5.0        // 5 point penalty for hard braking
            DrivingState.SUDDEN_ACCELERATION -> 4.0  // 4 point penalty for sudden acceleration
            DrivingState.SHARP_TURN -> 3.0          // 3 point penalty for sharp turns
            DrivingState.NORMAL -> -0.5             // 0.5 point reward for normal driving
        }

        // Apply bounds to ensure the score remains between 0 and 100
        currentScore = (currentScore - penalty).coerceIn(0.0, 100.0)
        
        return currentScore.toInt()
    }
}
