package com.example.smartdrivemonitor.data.repository

import com.example.smartdrivemonitor.data.source.VhalDataSource
import com.example.smartdrivemonitor.domain.model.SensorFrame
import com.example.smartdrivemonitor.domain.repository.VhalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VhalRepositoryImpl @Inject constructor(
    private val vhalDataSource: VhalDataSource
) : VhalRepository {


    override fun getSensorFusionStream(): Flow<SensorFrame> {
        return vhalDataSource.getSensorFusionStream()
    }
}
