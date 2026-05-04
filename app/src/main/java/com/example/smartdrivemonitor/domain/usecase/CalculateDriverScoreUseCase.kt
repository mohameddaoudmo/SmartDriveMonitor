package com.example.smartdrivemonitor.domain.usecase

import com.example.smartdrivemonitor.domain.model.DrivingState

class CalculateDriverScoreUseCase {

    // السكور بيبدأ من 100
    private var currentScore = 100.0

    operator fun invoke(state: DrivingState): Int {
        val penalty = when (state) {
            DrivingState.HARD_BRAKING -> 5.0        // خصم 5 نقط للفرملة العنيفة
            DrivingState.RAPID_ACCELERATION -> 4.0  // خصم 4 نقط للتسارع
            DrivingState.SHARP_TURN -> 3.0          // خصم 3 نقط للغرز
            DrivingState.NORMAL -> -0.5             // مكافأة نص نقطة للقيادة الآمنة
        }

        // تطبيق المعادلة الرياضية لضمان إن السكور مابين 0 و 100
        currentScore = (currentScore - penalty).coerceIn(0.0, 100.0)
        
        return currentScore.toInt()
    }
}
