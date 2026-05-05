package com.example.smartdrivemonitor.ml

import com.example.smartdrivemonitor.domain.model.SensorFrame
import com.example.smartdrivemonitor.ml.model.BehaviorResult
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorClassifier @Inject constructor(
    private val engine: MLInferenceEngine,
    private val normalizer: SensorNormalizer,
    private val featureExtractor: FeatureExtractor
) {
    // Sliding window — buffer of normalized feature vectors
    private val windowBuffer = LinkedList<FloatArray>()
    private var frameCount   = 0

    /**
     * Processes a SensorFrame by adding it to the sliding window.
     * Triggers model inference when the window is full and meets the stride criteria.
     */
    fun addFrame(frame: SensorFrame): BehaviorResult? {
        // 1. Extract derived features from raw sensor data
        val rawFeatures = featureExtractor.extract(frame)

        // 2. Normalize features using parameters synchronized with the Python training pipeline
        val normalized = normalizer.normalize(rawFeatures)

        // 3. Add to sliding window buffer
        windowBuffer.addLast(normalized)
        if (windowBuffer.size > ModelConfig.WINDOW_SIZE) {
            windowBuffer.removeFirst()
        }

        frameCount++

        // 4. Trigger inference based on window size and sliding stride (e.g., 50% overlap)
        val shouldInfer = windowBuffer.size == ModelConfig.WINDOW_SIZE &&
                          (frameCount % (ModelConfig.WINDOW_SIZE / 2) == 0)

        return if (shouldInfer) {
            engine.classify(windowBuffer.toList())
        } else null
    }

    fun reset() {
        windowBuffer.clear()
        frameCount = 0
        featureExtractor.reset()
    }
}
