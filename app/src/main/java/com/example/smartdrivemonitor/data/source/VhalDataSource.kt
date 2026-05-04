package com.example.smartdrivemonitor.data.source

import com.example.smartdrivemonitor.domain.model.SensorFrame

import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate

class VhalDataSource(private val carPropertyManager: CarPropertyManager) {

    // 1. دالة عامة لقراءة أي حساس وتحويله لـ Flow
    private fun observeProperty(propertyId: Int, updateRate: Float = CarPropertyManager.SENSOR_RATE_NORMAL): Flow<Float> = callbackFlow {
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (value.propertyId == propertyId) {
                    val sensorValue = value.value as? Float ?: 0f
                    trySend(sensorValue)
                }
            }

            override fun onErrorEvent(propId: Int, zone: Int) {
                close(Exception("Error reading VHAL property: $propId"))
            }
        }

        carPropertyManager.registerCallback(callback, propertyId, updateRate)

        // لما الـ Flow يتقفل، بنلغي الاشتراك عشان نمنع تسريب الذاكرة
        awaitClose { carPropertyManager.unregisterCallback(callback) }
    }.conflate() // conflate: لو البيانات جت بسرعة جداً، بناخد أحدث قراءة بس

    // 2. قراءة الحساسات بشكل منفصل
    val speedFlow = observeProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED)
    val rpmFlow = observeProperty(VehiclePropertyIds.ENGINE_RPM)
    val steeringFlow = observeProperty(VehiclePropertyIds.PERF_STEERING_ANGLE)
    val brakeFlow = observeProperty(VehiclePropertyIds.BRAKE_INPUT)

    // 3. دمج كل الحساسات في تيار بيانات واحد (Sensor Fusion)
    fun observeSensorFusion(): Flow<SensorFrame> {
        return combine(speedFlow, rpmFlow, steeringFlow, brakeFlow) { speed, rpm, steering, brake ->
            SensorFrame(
                speed = speed,
                rpm = rpm,
                steeringAngle = steering,
                brake = brake
            )
        }
    }
}
