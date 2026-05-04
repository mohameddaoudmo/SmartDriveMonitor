package com.example.smartdrivemonitor

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartdrivemonitor.data.repository.VhalRepositoryImpl
import com.example.smartdrivemonitor.data.source.DataLogger
import com.example.smartdrivemonitor.data.source.VhalDataSource
import com.example.smartdrivemonitor.domain.model.DrivingState
import com.example.smartdrivemonitor.ui.dashboard.SmartDriveViewModel

class MainActivity : ComponentActivity() {

    private var car: Car? = null
    private lateinit var viewModel: SmartDriveViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. تهيئة الاتصال بالسيارة وإنشاء الـ ViewModel
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            car = Car.createCar(this)
            val carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            
            if (carPropertyManager != null) {
                // Dependency Injection manual for now
                val dataSource = VhalDataSource(carPropertyManager)
                val repository = VhalRepositoryImpl(dataSource)
                val dataLogger = DataLogger(applicationContext)
                viewModel = SmartDriveViewModel(repository, dataLogger)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    if (::viewModel.isInitialized) {
                        DashboardScreen(viewModel)
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "Waiting for Automotive VHAL...", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        car?.disconnect()
    }
}

@Composable
fun DashboardScreen(viewModel: SmartDriveViewModel) {
    val carState by viewModel.carState.collectAsState()
    val drivingState by viewModel.drivingState.collectAsState()

    val driverScore by viewModel.driverScore.collectAsState()

    val (statusColor, statusText) = when (drivingState) {
        DrivingState.NORMAL -> Color(0xFF4CAF50) to "Safe Driving"
        DrivingState.HARD_BRAKING -> Color(0xFFF44336) to "⚠️ HARD BRAKING!"
        DrivingState.RAPID_ACCELERATION -> Color(0xFFFF9800) to "⚠️ RAPID ACCELERATION!"
        DrivingState.SHARP_TURN -> Color(0xFFE91E63) to "⚠️ SHARP TURN!"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // الـ Banner التحذيري
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.2f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = statusText, color = statusColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Driver Score: $driverScore", color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // شبكة الحساسات
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SensorCard("Speed", "${"%.1f".format(carState.speed)} m/s")
            SensorCard("RPM", "${carState.rpm.toInt()} rev/m")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SensorCard("Steering", "${"%.1f".format(carState.steeringAngle)}°")
            SensorCard("Brake", "${"%.0f".format(carState.brake)}%")
        }
    }
}

@Composable
fun SensorCard(title: String, value: String) {
    Card(
        modifier = Modifier.size(160.dp, 120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 16.sp, color = Color.Gray)
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}