package com.example.smartdrivemonitor.domain.model

enum class DrivingState {
    NORMAL,              // قيادة طبيعية وآمنة
    HARD_BRAKING,        // فرملة عنيفة/مفاجئة
    RAPID_ACCELERATION,  // تسارع مفاجئ (أمريكاني)
    SHARP_TURN           // غرزة أو ملف خطر على سرعة عالية
}
