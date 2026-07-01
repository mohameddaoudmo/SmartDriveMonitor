package com.smartcabin.domain.repository

import com.smartcabin.domain.model.SensorFrame
import kotlinx.coroutines.flow.Flow

interface VhalRepository {
    fun getSensorFusionStream(): Flow<SensorFrame>
}
