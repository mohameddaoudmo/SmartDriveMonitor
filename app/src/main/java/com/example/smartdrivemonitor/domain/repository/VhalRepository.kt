package com.example.smartdrivemonitor.domain.repository

import com.example.smartdrivemonitor.domain.model.SensorFrame
import kotlinx.coroutines.flow.Flow

interface VhalRepository {
    fun getSensorFusionStream(): Flow<SensorFrame>
}
