package com.example.smartdrivemonitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdrivemonitor.data.ml.TelemetryBuffer
import com.example.smartdrivemonitor.data.source.DataLogger
import com.example.smartdrivemonitor.domain.model.DrivingState
import com.example.smartdrivemonitor.domain.model.SensorFrame
import com.example.smartdrivemonitor.domain.repository.VhalRepository
import com.example.smartdrivemonitor.domain.usecase.AnalyzeDrivingBehaviorUseCase
import com.example.smartdrivemonitor.domain.usecase.CalculateDriverScoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmartDriveViewModel(
    private val repository: VhalRepository,
    private val dataLogger: DataLogger 
) : ViewModel() {

    private val _carState = MutableStateFlow(SensorFrame())
    val carState: StateFlow<SensorFrame> = _carState.asStateFlow()

    private val _drivingState = MutableStateFlow(DrivingState.NORMAL)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    // State جديد لعرض السكور على الواجهة
    private val _driverScore = MutableStateFlow(100)
    val driverScore: StateFlow<Int> = _driverScore.asStateFlow()

    private val telemetryBuffer = TelemetryBuffer(windowSize = 50)
    
    // Use Cases
    private val analyzeUseCase = AnalyzeDrivingBehaviorUseCase()
    private val scoringUseCase = CalculateDriverScoreUseCase()

    init {
        viewModelScope.launch {
            repository.getSensorFusionStream().collect { frame ->
                _carState.value = frame
            }
        }

        viewModelScope.launch {
            telemetryBuffer.bufferStream(repository.getSensorFusionStream())
                .collect { bufferWindow ->
                    // 1. تحليل السلوك باستخدام الـ Use Case
                    val state = analyzeUseCase(bufferWindow)
                    _drivingState.value = state
                    
                    // 2. حساب السكور باستخدام الـ Use Case
                    val score = scoringUseCase(state)
                    _driverScore.value = score
                    
                    // 3. حفظ البيانات
                    dataLogger.logBufferToCsv(bufferWindow, state)
                }
        }
    }
}
