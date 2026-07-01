package com.smartcabin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcabin.data.source.DataLogger
import com.smartcabin.domain.model.DrivingState
import com.smartcabin.domain.model.SensorFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import com.smartcabin.data.repository.TripRepositoryImpl
import com.smartcabin.domain.model.Trip
import com.smartcabin.domain.model.TripStatus
import javax.inject.Inject

@HiltViewModel
class SmartDriveViewModel @Inject constructor(
    private val dataLogger: DataLogger,
    private val tripRepository: TripRepositoryImpl,
    private val daemonRepository: com.smartcabin.domain.repository.DaemonRepository
) : ViewModel() {

    private val _carState = MutableStateFlow(SensorFrame())
    val carState: StateFlow<SensorFrame> = _carState.asStateFlow()

    private val _drivingState = MutableStateFlow(DrivingState.NORMAL)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    private val _driverScore = MutableStateFlow(100)
    val driverScore: StateFlow<Int> = _driverScore.asStateFlow()

    // Native Daemon State Flow
    val daemonState: StateFlow<com.smartcabin.data.source.NativeDaemonState?> = daemonRepository.daemonState

    private var runningScore = 100f
    
    // Trip Tracking
    private var currentTrip: Trip? = null
    private var stopTimerJob: Job? = null

    init {
        // Start native UDP daemon monitoring on port 5005
        daemonRepository.startMonitoring(5005)

        // Resume active trip if exists
        viewModelScope.launch {
            currentTrip = tripRepository.getActiveTrip()
        }
        startDataPipeline()
    }

    private fun startDataPipeline() {
        viewModelScope.launch(Dispatchers.Default) {
            daemonState.collect { state ->
                if (state == null) return@collect
                
                val frame = SensorFrame(
                    speed = state.fusedSpeed,
                    rpm = state.rpm,
                    steeringAngle = state.steeringAngle,
                    brake = state.brake,
                    timestamp = System.currentTimeMillis()
                )

                _carState.value = frame

                val riskScore = 100f - state.safetyScore
                
                val currentState = when {
                    riskScore > 75f -> DrivingState.HARD_BRAKING
                    riskScore > 50f -> DrivingState.SUDDEN_ACCELERATION
                    riskScore > 25f -> DrivingState.SHARP_TURN
                    else -> DrivingState.NORMAL
                }
                
                _drivingState.value = currentState
                _driverScore.value = state.safetyScore.toInt()

                // Trip Tracking Logic
                if (frame.speed > 1.4f) { // ~5 km/h
                    stopTimerJob?.cancel()
                    if (currentTrip == null) {
                        val newTrip = Trip(startTime = System.currentTimeMillis())
                        currentTrip = newTrip
                        viewModelScope.launch { tripRepository.insertTrip(newTrip) }
                    } else {
                        val trip = currentTrip!!
                        val newMaxSpeed = maxOf(trip.maxSpeed, frame.speed)
                        val newAvgSpeed = if (trip.avgSpeed == 0f) frame.speed else (trip.avgSpeed + frame.speed) / 2
                        currentTrip = trip.copy(
                            maxSpeed = newMaxSpeed, 
                            avgSpeed = newAvgSpeed, 
                            finalScore = state.safetyScore.toInt()
                        )
                    }
                } else if (frame.speed < 0.5f && currentTrip != null) {
                    if (stopTimerJob?.isActive != true) {
                        stopTimerJob = viewModelScope.launch {
                            delay(30000) // 30 seconds stop timeout
                            endCurrentTrip()
                        }
                    }
                }
            }
        }
    }

    private fun endCurrentTrip() {
        currentTrip?.let { trip ->
            viewModelScope.launch {
                val finalTrip = trip.copy(
                    endTime = System.currentTimeMillis(),
                    status = TripStatus.COMPLETED,
                    finalScore = _driverScore.value
                )
                tripRepository.updateTrip(finalTrip)
                currentTrip = null
                Log.d("SmartDriveVM", "Trip completed and saved: $finalTrip")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        daemonRepository.stopMonitoring()
        endCurrentTrip()
    }
}
