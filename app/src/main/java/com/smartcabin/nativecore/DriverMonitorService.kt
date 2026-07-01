package com.smartcabin.nativecore

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log
import com.smartcabin.data.source.NativeBridge
import kotlin.concurrent.thread

class DriverMonitorService : Service() {

    private val listeners = RemoteCallbackList<IDriverMonitorListener>()
    private var isRunning = false
    private var updateThread: Thread? = null

    private val binder = object : IDriverMonitorService.Stub() {
        override fun registerListener(listener: IDriverMonitorListener?) {
            if (listener != null) {
                listeners.register(listener)
            }
        }

        override fun unregisterListener(listener: IDriverMonitorListener?) {
            if (listener != null) {
                listeners.unregister(listener)
            }
        }

        override fun injectSensorData(speed: Float, brake: Float, steeringAngle: Float, rpm: Float) {
            // Forward to native daemon
            NativeBridge.injectDaemonSensorData(speed, brake, steeringAngle, rpm)
        }

        override fun getLatestState(): DriverStateParcelable {
            val state = NativeBridge.getLatestDaemonState()
            return if (state != null) {
                DriverStateParcelable().apply {
                    rawSpeed = state.rawSpeed
                    fusedSpeed = state.fusedSpeed
                    acceleration = state.acceleration
                    this.brake = state.brake
                    this.steeringAngle = state.steeringAngle
                    this.rpm = state.rpm
                    safetyScore = state.safetyScore
                    dominantRuleIndex = state.dominantRuleIndex
                    ruleText = state.ruleText
                    confidence = state.confidence
                    timestamp = state.timestamp
                }
            } else {
                DriverStateParcelable().apply { timestamp = System.currentTimeMillis() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DriverMonitorService onCreate in process: ${android.os.Process.myPid()}")
        
        // Start the native daemon
        NativeBridge.startDaemon(8080)
        isRunning = true
        
        // Start a thread to poll native state and broadcast to listeners
        updateThread = thread {
            while (isRunning) {
                try {
                    val state = binder.latestState
                    broadcastState(state)
                    Thread.sleep(100) // 10 Hz updates
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in update thread", e)
                }
            }
        }
    }

    private fun broadcastState(state: DriverStateParcelable) {
        val i = listeners.beginBroadcast()
        for (index in 0 until i) {
            try {
                listeners.getBroadcastItem(index).onDriverStateUpdated(state)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting state", e)
            }
        }
        listeners.finishBroadcast()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "DriverMonitorService onBind")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DriverMonitorService onDestroy")
        isRunning = false
        updateThread?.interrupt()
        NativeBridge.stopDaemon()
        listeners.kill()
    }

    companion object {
        private const val TAG = "DriverMonitorService"
    }
}
