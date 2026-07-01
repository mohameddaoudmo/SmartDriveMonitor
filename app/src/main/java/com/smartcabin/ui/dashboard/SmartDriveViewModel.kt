package com.smartcabin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcabin.data.ml.TelemetryBuffer
import com.smartcabin.data.source.DataLogger
import com.smartcabin.domain.model.DrivingState
import com.smartcabin.domain.model.SensorFrame
import com.smartcabin.domain.repository.VhalRepository
import com.smartcabin.domain.usecase.AnalyzeDrivingBehaviorUseCase
import com.smartcabin.domain.usecase.CalculateDriverScoreUseCase
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
import com.smartcabin.ml.BehaviorClassifier
import com.smartcabin.ml.model.BehaviorClass
import com.smartcabin.ml.model.BehaviorResult
import com.smartcabin.data.repository.TripRepositoryImpl
import com.smartcabin.domain.model.Trip
import com.smartcabin.domain.model.TripStatus
import javax.inject.Inject

@HiltViewModel
class SmartDriveViewModel @Inject constructor(
    private val repository: VhalRepository,
    private val dataLogger: DataLogger,
    private val classifier: BehaviorClassifier,
    private val tripRepository: TripRepositoryImpl,
    private val daemonRepository: com.smartcabin.domain.repository.DaemonRepository
) : ViewModel() {

    private val _carState = MutableStateFlow(SensorFrame())
    val carState: StateFlow<SensorFrame> = _carState.asStateFlow()

    private val _drivingState = MutableStateFlow(DrivingState.NORMAL)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    private val _driverScore = MutableStateFlow(100)
    val driverScore: StateFlow<Int> = _driverScore.asStateFlow()

    // ML Inference State
    private val _mlResult = MutableStateFlow<BehaviorResult?>(null)
    val mlResult: StateFlow<BehaviorResult?> = _mlResult.asStateFlow()

    // Native Daemon State Flow
    val daemonState: StateFlow<com.smartcabin.data.source.NativeDaemonState?> = daemonRepository.daemonState

    private val telemetryBuffer = TelemetryBuffer(windowSize = 50)
    
    // Legacy Use Cases (optional, can be kept for hybrid approach)
    private val analyzeUseCase = AnalyzeDrivingBehaviorUseCase()
    private val scoringUseCase = CalculateDriverScoreUseCase()

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
            repository.getSensorFusionStream().collect { frame ->
                // Inject VHAL data into native C++ daemon for processing
                daemonRepository.injectData(frame.speed, frame.brake, frame.steeringAngle, frame.rpm)

                // 1. Minimal logging to avoid performance hits
                Log.d("SmartDriveVM", "New Frame - Speed: ${frame.speed}, RPM: ${frame.rpm}")

                // 2. Log to CSV (Data Logging) - Calculate ground truth BEFORE updating carState
                val groundTruthState = deriveLabel(frame)
                dataLogger.logFrame(frame, groundTruthState)

                // 3. Update raw sensor UI
                _carState.value = frame

                // 4. Feed to ML classifier
                val result = classifier.addFrame(frame)
                result?.let { r ->
                    Log.i("SmartDrive_ML", "PREDICTION: ${r.behaviorClass} (Confidence: ${r.confidence})")
                    _mlResult.value = r
                    
                    val legacyState = when(r.behaviorClass) {
                        BehaviorClass.NORMAL -> DrivingState.NORMAL
                        BehaviorClass.SUDDEN_ACCELERATION -> DrivingState.SUDDEN_ACCELERATION
                        BehaviorClass.HARD_BRAKING -> DrivingState.HARD_BRAKING
                        BehaviorClass.SHARP_TURN -> DrivingState.SHARP_TURN
                        else -> DrivingState.NORMAL
                    }
                    _drivingState.value = legacyState

                    val impact = r.scoreImpact.toFloat()
                    runningScore = (runningScore * 0.95f + (100f + impact * 2) * 0.05f)
                        .coerceIn(0f, 100f)
                    _driverScore.value = runningScore.toInt()
                }

                // 5. Trip Tracking Logic
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
                            finalScore = runningScore.toInt()
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

    /**
     * Derives a driving state label based on sensor thresholds.
     * Used for ground truth labeling in CSV data collection.
     */
    private fun deriveLabel(frame: SensorFrame): DrivingState {
        val speedKmh = frame.speed * 3.6f
        
        // Calculate deceleration from previous frame in the UI state
        val prevSpeedKmh = _carState.value.speed * 3.6f
        val decelerationKmh = prevSpeedKmh - speedKmh // positive means slowing down

        return when {
            // Sudden Acceleration: High RPM + speed above 40 km/h
            frame.rpm > 3500f && speedKmh > 40f -> DrivingState.SUDDEN_ACCELERATION
            
            // Hard Braking: Sudden drop in speed (more than 15 km/h between frames)
            decelerationKmh > 10f && speedKmh < 20f -> DrivingState.HARD_BRAKING
            
            // Sharp Turn: High steering angle
            kotlin.math.abs(frame.steeringAngle) > 25f -> DrivingState.SHARP_TURN
            
            else -> DrivingState.NORMAL
        }
    }

    private fun endCurrentTrip() {
        currentTrip?.let { trip ->
            viewModelScope.launch {
                val finalTrip = trip.copy(
                    endTime = System.currentTimeMillis(),
                    status = TripStatus.COMPLETED,
                    finalScore = runningScore.toInt()
                )
                tripRepository.updateTrip(finalTrip)
                currentTrip = null
                Log.d("SmartDriveVM", "Trip completed and saved: $finalTrip")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifier.reset()
        daemonRepository.stopMonitoring()
        endCurrentTrip()
    }
}
