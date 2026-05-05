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
class MLInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normalizer: SensorNormalizer
) {

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null

    // Input buffer: [1, WINDOW_SIZE, N_FEATURES] float32
    private var inputBuffer: ByteBuffer? = null
    // Output buffer: [1, 4] float32 (softmax probabilities)
    private var outputBuffer: ByteBuffer? = null

    init {
        try {
            // ── Load TFLite model from assets
            val modelBuffer = FileUtil.loadMappedFile(context, ModelConfig.MODEL_FILENAME)

            // ── NNAPI Delegate for hardware acceleration
            nnApiDelegate = if (ModelConfig.USE_NNAPI) {
                try {
                    NnApiDelegate(NnApiDelegate.Options().apply {
                        executionPreference = NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "NNAPI not available, using CPU: ${e.message}")
                    null
                }
            } else null

            val options = Interpreter.Options().apply {
                numThreads = ModelConfig.NUM_THREADS
                nnApiDelegate?.let { addDelegate(it) }
            }

            interpreter = Interpreter(modelBuffer, options)

            val nFeatures = normalizer.nFeatures
            // [batch=1, window=50, features=11] × 4 bytes (float32)
            inputBuffer  = ByteBuffer.allocateDirect(1 * ModelConfig.WINDOW_SIZE * nFeatures * 4)
                .order(ByteOrder.nativeOrder())
            // [batch=1, classes=4] × 4 bytes
            outputBuffer = ByteBuffer.allocateDirect(1 * 4 * 4)
                .order(ByteOrder.nativeOrder())

            Log.i(TAG, "MLInferenceEngine initialized — model: ${ModelConfig.MODEL_FILENAME}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite: ${e.message}")
        }
    }

    /**
     * Performs model inference on a window of normalized feature vectors.
     */
    fun classify(window: List<FloatArray>): BehaviorResult? {
        val interp = interpreter ?: return null
        val inBuf = inputBuffer ?: return null
        val outBuf = outputBuffer ?: return null

        if (window.size < ModelConfig.WINDOW_SIZE) {
            return null
        }

        val startTime = System.currentTimeMillis()

        // ── Fill input buffer
        inBuf.clear()
        window.takeLast(ModelConfig.WINDOW_SIZE).forEach { features ->
            features.forEach { value -> inBuf.putFloat(value) }
        }
        inBuf.rewind()
        outBuf.clear()

        // ── Run inference
        interp.run(inBuf, outBuf)

        val inferenceMs = System.currentTimeMillis() - startTime

        // ── Parse output probabilities
        outBuf.rewind()
        val probs = FloatArray(4) { outBuf.float }

        val classIdx   = probs.indices.maxByOrNull { probs[it] } ?: 0
        val confidence = probs[classIdx]

        val behaviorClass = if (confidence >= ModelConfig.MIN_CONFIDENCE) {
            when (classIdx) {
                ModelConfig.CLASS_NORMAL -> BehaviorClass.NORMAL
                ModelConfig.CLASS_ACCEL  -> BehaviorClass.SUDDEN_ACCELERATION
                ModelConfig.CLASS_BRAKE  -> BehaviorClass.HARD_BRAKING
                ModelConfig.CLASS_TURN   -> BehaviorClass.SHARP_TURN
                else                     -> BehaviorClass.UNKNOWN
            }
        } else {
            BehaviorClass.UNKNOWN
        }

        return BehaviorResult(
            behaviorClass   = behaviorClass,
            confidence      = confidence,
            probabilities   = probs,
            inferenceTimeMs = inferenceMs
        )
    }

    fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }
}
