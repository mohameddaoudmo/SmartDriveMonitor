package com.smartcabin.data.source

object NativeBridge {
    init {
        try {
            System.loadLibrary("smartdrivemonitor")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("NativeBridge", "Failed to load smartdrivemonitor C++ library", e)
        }
    }

    // Kalman Filter bindings (Direct JNI Path)
    external fun initFilter(initialSpeed: Float)
    external fun updateFilter(measuredSpeed: Float, dt: Float): Float
    external fun getAcceleration(): Float

    // ANFIS Engine bindings (Direct JNI Path)
    external fun evaluateAnfis(
        speedVariance: Float,
        steeringAngle: Float,
        acceleration: Float
    ): NativeAnfisResult

    // Native Daemon Controller bindings (UDP Daemon Path)
    external fun startDaemon(port: Int): Boolean
    external fun stopDaemon()
    external fun isDaemonRunning(): Boolean
    external fun injectDaemonSensorData(speed: Float, brake: Float, steering: Float, rpm: Float)
    external fun getLatestDaemonState(): NativeDaemonState?
}
