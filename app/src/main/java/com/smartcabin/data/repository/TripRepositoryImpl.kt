package com.smartcabin.data.repository

import com.smartcabin.data.source.db.TripDao
import com.smartcabin.data.source.db.TripEntity
import com.smartcabin.domain.model.Trip
import com.smartcabin.domain.model.TripStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao
) {
    suspend fun insertTrip(trip: Trip): Long {
        val entity = TripEntity(
            startTime = trip.startTime,
            endTime = trip.endTime,
            distanceMeters = trip.distanceMeters,
            avgSpeed = trip.avgSpeed,
            maxSpeed = trip.maxSpeed,
            finalScore = trip.finalScore,
            status = trip.status.name
        )
        return tripDao.insertTrip(entity)
    }

    suspend fun updateTrip(trip: Trip) {
        val entity = TripEntity(
            id = trip.id,
            startTime = trip.startTime,
            endTime = trip.endTime,
            distanceMeters = trip.distanceMeters,
            avgSpeed = trip.avgSpeed,
            maxSpeed = trip.maxSpeed,
            finalScore = trip.finalScore,
            status = trip.status.name
        )
        tripDao.updateTrip(entity)
    }

    suspend fun getActiveTrip(): Trip? {
        val entity = tripDao.getActiveTrip() ?: return null
        return mapEntityToTrip(entity)
    }

    fun getAllTrips(): Flow<List<Trip>> {
        return tripDao.getAllTrips().map { entities ->
            entities.map { mapEntityToTrip(it) }
        }
    }

    private fun mapEntityToTrip(entity: TripEntity): Trip {
        return Trip(
            id = entity.id,
            startTime = entity.startTime,
            endTime = entity.endTime,
            distanceMeters = entity.distanceMeters,
            avgSpeed = entity.avgSpeed,
            maxSpeed = entity.maxSpeed,
            finalScore = entity.finalScore,
            status = TripStatus.valueOf(entity.status)
        )
    }
}
