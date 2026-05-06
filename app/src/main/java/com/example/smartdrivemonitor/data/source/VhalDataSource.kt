package com.example.smartdrivemonitor.data.source

import com.example.smartdrivemonitor.domain.model.SensorFrame
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import kotlinx.coroutines.flow.*
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VhalDataSource @Inject constructor(
    private val carPropertyManager: CarPropertyManager
) {
    // PERF_VEHICLE_SPEED (0x11600207) - الوحيد المتاح غالباً للتطبيقات العادية
    private val SPEED_PROP = 291504647

    // Stream للسرعة الحقيقية القادمة من VHAL
    private val _realSpeed = MutableStateFlow(0f)

    init {
        setupSpeedSubscription()
    }

    private fun setupSpeedSubscription() {
        Log.d("VhalDataSource", "Attempting to subscribe to SPEED_PROP (0x${SPEED_PROP.toString(16)})")
        try {
            val callback = object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(value: CarPropertyValue<*>) {
                    Log.d("VhalDataSource", "VHAL Event Received: Prop=0x${value.propertyId.toString(16)}, Value=${value.value}")
                    if (value.propertyId == SPEED_PROP) {
                        val rawValue = value.value
                        val speedVal = when (rawValue) {
                            is Float -> rawValue
                            is Int -> rawValue.toFloat()
                            is Double -> rawValue.toFloat()
                            else -> 0f
                        }
                        _realSpeed.value = kotlin.math.abs(speedVal)
                    }
                }
                override fun onErrorEvent(propId: Int, areaId: Int) {
                    Log.e("VhalDataSource", "VHAL Error: Prop=0x${propId.toString(16)}, Area=$areaId")
                }
            }
            val success = carPropertyManager.registerCallback(
                callback, 
                SPEED_PROP, 
                CarPropertyManager.SENSOR_RATE_FAST
            )
            Log.d("VhalDataSource", "Registration result: $success")
        } catch (e: Exception) {
            Log.e("VhalDataSource", "CRITICAL: VHAL Speed sub failed", e)
        }
    }

    fun getSensorFusionStream(): Flow<SensorFrame> {
        var prevSpeed = 0f
        var prevTime = System.currentTimeMillis()

        return _realSpeed
            .sample(100) // تحديث كل 100ms (10Hz) ليناسب الموديل
            .map { currentSpeed ->
                val now = System.currentTimeMillis()
                val dt = ((now - prevTime) / 1000f).coerceAtLeast(0.01f)

                // ── حساب القيم المشتقة (Physics-based derivation)
                val accel = (currentSpeed - prevSpeed) / dt
                val decel = (prevSpeed - currentSpeed) / dt
                
                val derivedRpm = deriveRpm(currentSpeed, accel)
                val derivedBrake = (decel / 8f).coerceIn(0f, 1f)
                val derivedSteer = 0f // لا يمكن اشتقاقه من السرعة

                prevSpeed = currentSpeed
                prevTime = now

                SensorFrame(
                    speed = currentSpeed,
                    rpm = derivedRpm,
                    steeringAngle = derivedSteer,
                    brake = derivedBrake,
                    gear = deriveGear(currentSpeed),
                    timestamp = now
                )
            }
    }

    /**
     * حساب RPM تقديري بناءً على السرعة والتسارع لمحاكاة سلوك المحرك
     */
    private fun deriveRpm(speedMs: Float, accelMs2: Float): Float {
        val baseRpm = speedMs * 120f // تناسب طردي مع السرعة
        val accelBoost = (accelMs2 * 300f).coerceAtLeast(0f) // زيادة RPM عند التسارع
        return (800f + baseRpm + accelBoost).coerceIn(800f, 6500f)
    }

    /**
     * حساب الغيار التقديري بناءً على السرعة
     */
    private fun deriveGear(speedMs: Float): Int {
        val kmh = speedMs * 3.6f
        return when {
            kmh < 15  -> 1
            kmh < 30  -> 2
            kmh < 50  -> 3
            kmh < 80  -> 4
            kmh < 110 -> 5
            else      -> 6
        }
    }
}
