package com.example.smartdrivemonitor

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartdrivemonitor.data.repository.VhalRepositoryImpl
import com.example.smartdrivemonitor.data.source.DataLogger
import com.example.smartdrivemonitor.data.source.VhalDataSource
import com.example.smartdrivemonitor.domain.model.DrivingState
import com.example.smartdrivemonitor.ui.dashboard.SmartDriveViewModel
import com.example.smartdrivemonitor.ui.history.TripHistoryScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var car: Car? = null
    private val viewModel: SmartDriveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    val permissions = arrayOf(
                        "android.car.permission.CAR_SPEED",
                        "android.car.permission.CAR_ENGINE_DETAILED",
                        "android.car.permission.CAR_INFO",
                        "android.car.permission.CAR_BRAKE",
                        "android.car.permission.CAR_POWERTRAIN"
                    )
                    
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ ->
                        // Handle result if needed
                    }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(Unit) {
                        val missingPermissions = permissions.filter {
                            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missingPermissions.isNotEmpty()) {
                            permissionLauncher.launch(missingPermissions.toTypedArray())
                        }
                    }

                    var currentScreen by remember { mutableStateOf("Dashboard") }

                    if (currentScreen == "Dashboard") {
                        DashboardScreen(viewModel, onHistoryClick = { currentScreen = "History" })
                    } else {
                        TripHistoryScreen(onBackClick = { currentScreen = "Dashboard" })
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
fun DashboardScreen(viewModel: SmartDriveViewModel, onHistoryClick: () -> Unit) {
    val carState by viewModel.carState.collectAsState()
    val drivingState by viewModel.drivingState.collectAsState()
    val driverScore by viewModel.driverScore.collectAsState()

    val (statusColor, statusText) = when (drivingState) {
        DrivingState.NORMAL -> Color(0xFF00E676) to "SAFE DRIVING"
        DrivingState.HARD_BRAKING -> Color(0xFFFF1744) to "HARD BRAKING"
        DrivingState.RAPID_ACCELERATION -> Color(0xFFFF9100) to "RAPID ACCEL"
        DrivingState.SHARP_TURN -> Color(0xFFD500F9) to "SHARP TURN"
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541))
        )
    )) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing AI Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(2.dp, statusColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = statusText, color = statusColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "DRIVER SCORE: $driverScore", color = Color.White.copy(alpha = 0.9f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onHistoryClick, 
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A506B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("VIEW TRIP HISTORY", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphism Sensor Cards
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SensorCard("SPEED", "${"%.0f".format(carState.speed * 3.6f)}", "km/h")
                SensorCard("RPM", "${carState.rpm.toInt()}", "rev/m")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SensorCard("STEERING", "${"%.1f".format(carState.steeringAngle)}°", "angle")
                SensorCard("BRAKE", "${"%.0f".format(carState.brake * 100f)}%", "pressure")
            }
        }
    }
}

@Composable
fun SensorCard(title: String, value: String, unit: String) {
    Box(
        modifier = Modifier
            .size(170.dp, 130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}