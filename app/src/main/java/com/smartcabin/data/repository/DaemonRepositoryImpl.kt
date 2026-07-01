package com.smartcabin.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.smartcabin.data.source.NativeDaemonState
import com.smartcabin.domain.repository.DaemonRepository
import com.smartcabin.nativecore.DriverStateParcelable
import com.smartcabin.nativecore.IDriverMonitorListener
import com.smartcabin.nativecore.IDriverMonitorService
import com.smartcabin.nativecore.DriverMonitorService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DaemonRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DaemonRepository {

    private val _daemonState = MutableStateFlow<NativeDaemonState?>(null)
    override val daemonState: StateFlow<NativeDaemonState?> = _daemonState.asStateFlow()

    private var driverMonitorService: IDriverMonitorService? = null
    private var isBound = false

    private val aidlListener = object : IDriverMonitorListener.Stub() {
        override fun onDriverStateUpdated(state: DriverStateParcelable) {
            val daemonState = NativeDaemonState(
                rawSpeed = state.rawSpeed,
                fusedSpeed = state.fusedSpeed,
                acceleration = state.acceleration,
                brake = state.brake,
                steeringAngle = state.steeringAngle,
                rpm = state.rpm,
                safetyScore = state.safetyScore,
                dominantRuleIndex = state.dominantRuleIndex,
                ruleText = state.ruleText,
                confidence = state.confidence,
                timestamp = state.timestamp
            )
            _daemonState.value = daemonState
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i("DaemonRepository", "Connected to Isolated Daemon Service")
            driverMonitorService = IDriverMonitorService.Stub.asInterface(service)
            isBound = true
            try {
                driverMonitorService?.registerListener(aidlListener)
            } catch (e: Exception) {
                Log.e("DaemonRepository", "Failed to register listener", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i("DaemonRepository", "Disconnected from Isolated Daemon Service")
            driverMonitorService = null
            isBound = false
        }
    }

    override fun startMonitoring(port: Int): Boolean {
        Log.i("DaemonRepository", "Binding to Isolated Daemon Service")
        val intent = Intent(context, DriverMonitorService::class.java)
        // We use BIND_AUTO_CREATE so the service starts if not already running
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun stopMonitoring() {
        Log.i("DaemonRepository", "Unbinding from Isolated Daemon Service")
        if (isBound) {
            try {
                driverMonitorService?.unregisterListener(aidlListener)
            } catch (e: Exception) {
                Log.e("DaemonRepository", "Failed to unregister listener", e)
            }
            context.unbindService(serviceConnection)
            isBound = false
            driverMonitorService = null
        }
    }

    override fun injectData(speed: Float, brake: Float, steering: Float, rpm: Float) {
        if (isBound) {
            try {
                driverMonitorService?.injectSensorData(speed, brake, steering, rpm)
            } catch (e: Exception) {
                Log.e("DaemonRepository", "Failed to inject data via AIDL", e)
            }
        }
    }
}
