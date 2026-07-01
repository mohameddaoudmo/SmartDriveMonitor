package com.smartcabin.domain.repository

import com.smartcabin.data.source.NativeDaemonState
import kotlinx.coroutines.flow.StateFlow

interface DaemonRepository {
    val daemonState: StateFlow<NativeDaemonState?>
    fun startMonitoring(port: Int = 5005): Boolean
    fun stopMonitoring()
    fun injectData(speed: Float, brake: Float, steering: Float, rpm: Float)
}
