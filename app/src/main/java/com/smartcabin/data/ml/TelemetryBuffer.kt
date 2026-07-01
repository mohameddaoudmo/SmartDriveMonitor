package com.smartcabin.data.ml

import com.smartcabin.domain.model.SensorFrame

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.runningFold

class TelemetryBuffer(private val windowSize: Int) {

    /**
     * Converts a stream of individual frames into a sliding window of collected readings.
     */
    fun bufferStream(flow: Flow<SensorFrame>): Flow<List<SensorFrame>> {
        return flow.runningFold(emptyList<SensorFrame>()) { accumulator, value ->
            (accumulator + value).takeLast(windowSize)
        }.filter { it.size == windowSize }
    }
}
