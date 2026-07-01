package com.smartcabin.data.repository

import com.smartcabin.data.source.VhalDataSource
import com.smartcabin.domain.model.SensorFrame
import com.smartcabin.domain.repository.VhalRepository
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
