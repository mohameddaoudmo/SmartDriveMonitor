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
import kotlinx.coroutines.flow.onStart
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VhalDataSource @Inject constructor(private val carPropertyManager: CarPropertyManager) {

    private val _sharedFlows = mutableMapOf<Int, Flow<Float>>()

    // VHAL Property IDs
    // Hardcoded to ensure compilation across different Android Automotive SDK versions
    private val SPEED_PROP = 291504647        // PERF_VEHICLE_SPEED
    private val RPM_PROP = 291504901          // ENGINE_RPM
    private val STEERING_PROP = 291504386     // PERF_STEERING_ANGLE
    private val BRAKE_PROP = 291505152        // BRAKE_PEDAL_POSITION
    private val GEAR_PROP = 289408000         // GEAR_SELECTION

    private fun observeProperty(propertyId: Int, updateRate: Float): Flow<Float> {
        return _sharedFlows.getOrPut(propertyId) {
            callbackFlow {
                val callback = object : CarPropertyManager.CarPropertyEventCallback {
                    override fun onChangeEvent(value: CarPropertyValue<*>) {
                        if (value.propertyId == propertyId) {
                            val rawValue = value.value
                            val sensorValue = when (rawValue) {
                                is Number -> rawValue.toFloat()
                                is Boolean -> if (rawValue) 1f else 0f
                                else -> 0f
                            }
                            // Log commented out to prevent flooding logcat at 10Hz
                            // Log.d("VhalDataSource", "Property $propertyId changed: $sensorValue")
                            trySend(sensorValue)
                        }
                    }

                    override fun onErrorEvent(propId: Int, zone: Int) {
                        Log.e("VhalDataSource", "Error on property $propId")
                    }
                }

                try {
                    val success = carPropertyManager.registerCallback(callback, propertyId, updateRate)
                    if (!success) {
                        Log.w("VhalDataSource", "Property $propertyId registration failed.")
                    }
                } catch (e: Exception) {
                    Log.e("VhalDataSource", "Failed to register $propertyId", e)
                }

                awaitClose {
                    Log.d("VhalDataSource", "Unregistering $propertyId")
                    carPropertyManager.unregisterCallback(callback)
                }
            }.onStart { emit(0f) }.conflate()
        }
    }

    // Separate property flows using defined constants
    val speedFlow = observeProperty(SPEED_PROP, CarPropertyManager.SENSOR_RATE_NORMAL)
    val rpmFlow = observeProperty(RPM_PROP, CarPropertyManager.SENSOR_RATE_NORMAL)
    val steeringFlow = observeProperty(STEERING_PROP, CarPropertyManager.SENSOR_RATE_NORMAL)
    val brakeFlow = observeProperty(BRAKE_PROP, CarPropertyManager.SENSOR_RATE_ONCHANGE)
    val gearFlow = observeProperty(GEAR_PROP, CarPropertyManager.SENSOR_RATE_ONCHANGE)

    /**
     * Combines all individual property flows into a single SensorFrame stream (Sensor Fusion).
     */
    fun getSensorFusionStream(): Flow<SensorFrame> {
        return combine(speedFlow, rpmFlow, steeringFlow, brakeFlow, gearFlow) { speed, rpm, steering, brake, gear ->
            SensorFrame(
                speed = speed,
                rpm = rpm,
                steeringAngle = steering,
                brake = brake,
                gear = gear.toInt(),
                timestamp = System.currentTimeMillis() // Adding timestamp for model temporal features
            )
        }
    }
}
