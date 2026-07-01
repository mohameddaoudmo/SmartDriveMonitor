package com.smartcabin.ml

import android.content.Context
import org.json.JSONObject
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log

@Singleton
class SmartDriveProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val windowSize = 50
    private val numFeatures = 11

    // مصفوفات لحفظ مقاييس التوحيد
    private val mean = FloatArray(numFeatures)
    private val scale = FloatArray(numFeatures)

    // الطابور الدائري (Ring Buffer)
    private val windowBuffer = ArrayDeque<FloatArray>(windowSize)

    // متغيرات لحفظ القراءات السابقة لحساب معدل التغير (Deltas)
    private var prevSpeed = 0f
    private var prevRpm = 0f
    private var prevSpeedDelta = 0f

    init {
        try {
            // قراءة ملف JSON من الـ Assets عند التهيئة
            val jsonString = context.assets.open("scaler_meta.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val meanArray = jsonObject.getJSONArray("mean")
            val scaleArray = jsonObject.getJSONArray("scale")

            for (i in 0 until numFeatures) {
                mean[i] = meanArray.getDouble(i).toFloat()
                scale[i] = scaleArray.getDouble(i).toFloat()
            }
        } catch (e: Exception) {
            android.util.Log.e("SmartDriveProcessor", "Failed to load scaler_meta.json: ${e.message}")
        }
    }

    /**
     * تستقبل هذه الدالة القراءات الخام لحظياً من السيارة.
     * @return FloatArray مسطح بحجم 550 (50 * 11) جاهز للموديل، أو null إذا لم يكتمل الطابور بعد.
     */
    fun processFrame(
        speedMs: Float, 
        rpm: Float, 
        steeringAngle: Float, 
        brakeInput: Float, 
        gearNorm: Float = 0.5f // قيمة افتراضية للغيار كما كانت في البايثون
    ): FloatArray? {
        
        // 1. Feature Engineering (حساب الخصائص الجديدة)
        // منع الـ Spikes في أول فريم
        if (windowBuffer.isEmpty()) {
            prevSpeed = speedMs
            prevRpm = rpm
        }

        val speedDelta = speedMs - prevSpeed
        val rpmDelta = rpm - prevRpm
        val absSteering = Math.abs(steeringAngle)
        val jerk = speedDelta - prevSpeedDelta
        val brakeXSpeed = brakeInput * speedMs
        val rpmPerSpeed = if (speedMs > 0.5f) rpm / speedMs else 0f

        // تحديث المتغيرات للـ Frame القادم
        prevSpeed = speedMs
        prevRpm = rpm
        prevSpeedDelta = speedDelta

        // ترتيب الخصائص يجب أن يكون متطابقاً تماماً مع n_features في ملف JSON
        val features = floatArrayOf(
            speedMs, 
            rpm, 
            steeringAngle, 
            brakeInput,
            speedDelta, 
            rpmDelta, 
            absSteering, 
            jerk,
            brakeXSpeed, 
            rpmPerSpeed, 
            gearNorm
        )
        
        // Log one frame every 50 to see what's going into the model
        if (windowBuffer.size % 50 == 0) {
            Log.d("SmartDrive_Proc", "Raw Features: ${features.joinToString(", ")}")
        }

        // 2. Scaling (تطبيق معادلة التوحيد)
        for (i in 0 until numFeatures) {
            features[i] = (features[i] - mean[i]) / scale[i]
        }

        // 3. Windowing (إدارة الطابور الدائري)
        if (windowBuffer.size == windowSize) {
            windowBuffer.removeFirst() // إزالة أقدم قراءة
        }
        windowBuffer.addLast(features) // إضافة القراءة الجديدة

        // 4. تسليم البيانات إذا اكتملت الـ Window
        if (windowBuffer.size == windowSize) {
            // تحويل الـ Deque إلى مصفوفة مسطحة 1D تناسب TensorFlow Lite
            val flatArray = FloatArray(windowSize * numFeatures)
            var index = 0
            for (frame in windowBuffer) {
                for (value in frame) {
                    flatArray[index++] = value
                }
            }
            return flatArray
        }
        
        return null // الطابور لم يكتمل 50 قراءة بعد
    }
}
