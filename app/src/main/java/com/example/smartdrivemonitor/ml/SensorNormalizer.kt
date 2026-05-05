package com.example.smartdrivemonitor.ml

import android.content.Context
import org.json.JSONObject

class SensorNormalizer(context: Context) {

    private val mean: FloatArray
    private val scale: FloatArray
    val featureNames: List<String>

    init {
        // Load normalization metadata from assets
        // This ensures the features are scaled exactly as they were during model training.
        val json = try {
            context.assets.open(ModelConfig.SCALER_META_FILE)
                .bufferedReader().readText()
        } catch (e: Exception) {
            // Fallback mock metadata if file missing (for initial build)
            "{ 'mean': [0,0,0,0,0,0,0,0,0,0,0], 'scale': [1,1,1,1,1,1,1,1,1,1,1], 'features': [] }"
        }
        
        val meta = JSONObject(json)

        val meanArr = meta.getJSONArray("mean")
        val scaleArr = meta.getJSONArray("scale")
        val featArr = meta.getJSONArray("features")

        mean  = FloatArray(meanArr.length()) { meanArr.getDouble(it).toFloat() }
        scale = FloatArray(scaleArr.length()) { scaleArr.getDouble(it).toFloat() }
        featureNames = List(featArr.length()) { featArr.getString(it) }

        require(mean.size == scale.size) {
            "scaler_meta.json corrupt: mean/scale size mismatch"
        }
    }

    val nFeatures: Int get() = mean.size

    /**
     * Applies Z-Score normalization: (x - mean) / standard_deviation
     * Equivalent to StandardScaler.transform() in scikit-learn.
     */
    fun normalize(rawFeatures: FloatArray): FloatArray {
        if (rawFeatures.size != mean.size) return rawFeatures // Avoid crash if mismatch
        
        return FloatArray(mean.size) { i ->
            (rawFeatures[i] - mean[i]) / (scale[i] + 1e-8f)
        }
    }
}
