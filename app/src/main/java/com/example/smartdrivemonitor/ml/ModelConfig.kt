package com.example.smartdrivemonitor.ml

object ModelConfig {
    // Assets paths
    const val MODEL_FILENAME    = "DriverBehavior.tflite"
    const val SCALER_META_FILE  = "scaler_meta.json"

    // Window parameters - Must match the Python training configuration
    const val WINDOW_SIZE       = 50         // 50 frames = 5 seconds @ 10Hz
    const val INFERENCE_HZ      = 2          // Inference frequency (2 Hz)
    const val INFERENCE_INTERVAL_MS = 500L

    // Class indices - Must match the CLASS_NAMES index in the Python script
    const val CLASS_NORMAL      = 0
    const val CLASS_ACCEL       = 1
    const val CLASS_BRAKE       = 2
    const val CLASS_TURN        = 3

    val CLASS_NAMES = listOf("NORMAL", "SUDDEN_ACCELERATION", "HARD_BRAKING", "SHARP_TURN")

    // Confidence threshold - Probabilities below this will result in UNKNOWN classification
    const val MIN_CONFIDENCE    = 0.60f

    // TFLite thread config
    const val NUM_THREADS       = 2
    const val USE_NNAPI         = true   // Neural Network API for hardware acceleration
    const val USE_GPU_DELEGATE  = false  // Requires GPU delegate dependency to be enabled in Gradle
}
