package com.example.smartdrivemonitor.ml

import android.content.Context
import android.util.Log
import com.example.smartdrivemonitor.ml.model.BehaviorClass
import com.example.smartdrivemonitor.ml.model.BehaviorResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MLInferenceEngine"

@Singleton
class MLInferenceEngine    @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processor: SmartDriveProcessor
) {

    private var interpreter: Interpreter? = null

    // Input buffer: [1, WINDOW_SIZE, N_FEATURES] float32
    private var inputBuffer: ByteBuffer? = null
    // Output buffer: [1, 4] float32 (softmax probabilities)
    private var outputBuffer: ByteBuffer? = null

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, ModelConfig.MODEL_FILENAME)
            val options = Interpreter.Options().apply {
                numThreads = ModelConfig.NUM_THREADS
            }

            try {
                interpreter = Interpreter(modelBuffer, options)
                Log.i(TAG, "TFLite model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Interpreter creation failed: ${e.message}")
            }

            // [batch=1, window=50, features=11] × 4 bytes (float32)
            inputBuffer  = ByteBuffer.allocateDirect(1 * 50 * 11 * 4)
                .order(ByteOrder.nativeOrder())
            outputBuffer = ByteBuffer.allocateDirect(1 * 4 * 4)
                .order(ByteOrder.nativeOrder())

            Log.i(TAG, "MLInferenceEngine initialized with SmartDriveProcessor")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite: ${e.message}")
        }
    }

    /**
     * Performs model inference using raw sensor data.
     */
    fun processAndClassify(
        speedMs: Float,
        rpm: Float,
        steering: Float,
        brake: Float,
        gearNorm: Float = 0.5f
    ): BehaviorResult? {
        val interp = interpreter ?: return null
        val inBuf = inputBuffer ?: return null
        val outBuf = outputBuffer ?: return null

        // 1. Process frame and get flat window array [550]
        val flatWindow = processor.processFrame(speedMs, rpm, steering, brake, gearNorm) ?: return null

        val startTime = System.currentTimeMillis()

        // 2. Fill input buffer
        inBuf.clear()
        flatWindow.forEach { inBuf.putFloat(it) }
        inBuf.rewind()
        outBuf.clear()

        // 3. Run inference
        try {
            interp.run(inBuf, outBuf)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            return null
        }

        val inferenceMs = System.currentTimeMillis() - startTime

        // 4. Parse output
        outBuf.rewind()
        val probs = FloatArray(4) { outBuf.float }

        val classIdx   = probs.indices.maxByOrNull { probs[it] } ?: 0
        val confidence = probs[classIdx]

        val behaviorClass = when (classIdx) {
            ModelConfig.CLASS_NORMAL -> BehaviorClass.NORMAL
            ModelConfig.CLASS_ACCEL  -> BehaviorClass.SUDDEN_ACCELERATION
            ModelConfig.CLASS_BRAKE  -> BehaviorClass.HARD_BRAKING
            ModelConfig.CLASS_TURN   -> BehaviorClass.SHARP_TURN
            else                     -> BehaviorClass.UNKNOWN
        }

        val resultClass = if (confidence >= ModelConfig.MIN_CONFIDENCE) behaviorClass else BehaviorClass.UNKNOWN

        Log.d("SmartDrive_ML", "Threshold: ${ModelConfig.MIN_CONFIDENCE}, Confidence: $confidence")
        Log.i("SmartDrive_ML", "RAW PROBS: [${probs.joinToString(", ")}]")
        Log.i("SmartDrive_ML", "BEST GUESS: $behaviorClass")
        
        return BehaviorResult(
            behaviorClass   = resultClass,
            confidence      = confidence,
            probabilities   = probs,
            inferenceTimeMs = inferenceMs
        )
    }

    fun close() {
        interpreter?.close()
    }
}
