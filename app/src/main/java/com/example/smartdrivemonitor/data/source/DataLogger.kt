package com.example.smartdrivemonitor.data.source

import com.example.smartdrivemonitor.domain.model.DrivingState
import com.example.smartdrivemonitor.domain.model.SensorFrame
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataLogger @Inject constructor(@ApplicationContext private val context: Context) {

    // Target file for logging telemetry data
    private val fileName = "driving_data.csv"

    fun logBufferToCsv(frames: List<SensorFrame>, label: DrivingState) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            val isNewFile = !file.exists()

            FileWriter(file, true).use { writer ->
                if (isNewFile) {
                    writer.append("timestamp_ms,speed_ms,rpm,steering_angle,brake_input,gear,label\n")
                }

                val labelString = when(label) {
                    DrivingState.NORMAL -> "NORMAL"
                    DrivingState.SUDDEN_ACCELERATION -> "SUDDEN_ACCELERATION"
                    DrivingState.HARD_BRAKING -> "HARD_BRAKING"
                    DrivingState.SHARP_TURN -> "SHARP_TURN"
                }

                frames.forEach { frame ->
                    writer.append("${frame.timestamp},${frame.speed},${frame.rpm},${frame.steeringAngle},${frame.brake},${frame.gear},${labelString}\n")
                }
            }
            // Log.d("SmartDrive_Data", "Saved ${frames.size} frames with label: ${label.name} to CSV at: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("SmartDrive_Data", "Error saving data to CSV: ${e.message}")
        }
    }

    fun logFrame(frame: SensorFrame, label: DrivingState) {
        logBufferToCsv(listOf(frame), label)
    }
}
