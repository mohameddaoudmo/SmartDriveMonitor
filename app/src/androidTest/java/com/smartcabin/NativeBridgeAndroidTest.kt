package com.smartcabin

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartcabin.data.source.NativeBridge
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeBridgeAndroidTest {

    @Test
    fun testNativeLibraryLoad() {
        // Simple call to ensure library is loaded and JNI function is found
        try {
            NativeBridge.initFilter(0f)
            assertTrue(true)
        } catch (e: UnsatisfiedLinkError) {
            fail("Native library smartdrivemonitor failed to load: ${e.message}")
        }
    }

    @Test
    fun testKalmanFilterSmoothing() {
        // Initializing filter with 10.0 m/s
        NativeBridge.initFilter(10.0f)
        
        // Feed noisy measurements converging to 20.0 m/s
        var filteredSpeed = 10f
        val dt = 0.1f // 100ms interval
        
        for (i in 0 until 10) {
            filteredSpeed = NativeBridge.updateFilter(20.0f, dt)
        }
        
        // The speed should have moved towards 20.0 m/s but be smoothed
        assertTrue(filteredSpeed > 15f && filteredSpeed < 21f)
        
        // Acceleration should be positive
        val acc = NativeBridge.getAcceleration()
        assertTrue("Acceleration should be positive: $acc", acc > 0f)
    }

    @Test
    fun testAnfisEngineSafeScenario() {
        // Safe driving inputs: Low variance (0.5), low steering (2.0), low acceleration (0.2)
        val result = NativeBridge.evaluateAnfis(
            speedVariance = 0.5f,
            steeringAngle = 2.0f,
            acceleration = 0.2f
        )
        
        assertNotNull(result)
        // Score should be very low (close to 0)
        assertTrue("Safe safety score should be low (< 20): ${result.safetyScore}", result.safetyScore < 20f)
        // Check confidence
        assertTrue(result.confidence > 0f)
        // Check XAI text contains low risk label
        assertTrue(result.ruleText.contains("Risk level is LOW"))
    }

    @Test
    fun testAnfisEngineDangerousScenario() {
        // Dangerous driving inputs: High variance (15.0), sharp steering (45.0), harsh acceleration (4.5)
        val result = NativeBridge.evaluateAnfis(
            speedVariance = 15.0f,
            steeringAngle = 45.0f,
            acceleration = 4.5f
        )
        
        assertNotNull(result)
        // Score should be high (> 75)
        assertTrue("Dangerous safety score should be high (> 75): ${result.safetyScore}", result.safetyScore > 75f)
        // Check XAI text contains high risk label
        assertTrue(result.ruleText.contains("Risk level is HIGH"))
    }
}
