package com.example.smartdrivemonitor.data.ml

import com.example.smartdrivemonitor.domain.model.SensorFrame

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.runningFold

class TelemetryBuffer(private val windowSize: Int) {

    /**
     * يحول تيار البيانات الفردي إلى "نوافذ" (Windows) من القراءات المجمعة.
     */
    fun bufferStream(flow: Flow<SensorFrame>): Flow<List<SensorFrame>> {
        return flow.runningFold(emptyList<SensorFrame>()) { accumulator, value ->
            (accumulator + value).takeLast(windowSize)
        }.filter { it.size == windowSize }
    }
}
