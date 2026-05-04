package com.example.smartdrivemonitor.data.source

import com.example.smartdrivemonitor.domain.model.DrivingState
import com.example.smartdrivemonitor.domain.model.SensorFrame

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter

class DataLogger(private val context: Context) {

    // اسم الملف اللي هنجمع فيه الداتا
    private val fileName = "driving_data.csv"

    fun logBufferToCsv(frames: List<SensorFrame>, label: DrivingState) {
        try {
            val file = File(context.filesDir, fileName)
            val isNewFile = !file.exists()

            // بنفتح الملف في وضع الـ Append عشان نكمل كتابة عليه مش نمسحه
            FileWriter(file, true).use { writer ->
                // لو الملف جديد، نكتب العناوين فوق (الهيدر)
                if (isNewFile) {
                    writer.append("timestamp,speed,rpm,steeringAngle,brake,label\n")
                }

                // نلف على الـ 50 قراءة ونكتبهم في الملف
                frames.forEach { frame ->
                    writer.append("${frame.timestamp},${frame.speed},${frame.rpm},${frame.steeringAngle},${frame.brake},${label.name}\n")
                }
            }
            Log.d("SmartDrive_Data", "Saved ${frames.size} frames with label: ${label.name} to CSV.")
        } catch (e: Exception) {
            Log.e("SmartDrive_Data", "Error saving data to CSV: ${e.message}")
        }
    }
}
