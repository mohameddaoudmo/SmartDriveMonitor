package com.smartcabin

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
import com.smartcabin.data.source.DataLogger
import com.smartcabin.domain.model.DrivingState
import com.smartcabin.ui.dashboard.SmartDriveViewModel
import com.smartcabin.ui.history.TripHistoryScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset

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
                        "android.car.permission.READ_STEERING_STATE",
                        "android.car.permission.CAR_STEERING",
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
                        com.smartcabin.ui.dashboard.SmartCabinDashboard(viewModel, onHistoryClick = { currentScreen = "History" })
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
    val daemonState by viewModel.daemonState.collectAsState()
    val speedHistory = remember { mutableStateListOf<Pair<Float, Float>>() }

    LaunchedEffect(daemonState) {
        daemonState?.let { ds ->
            // Convert m/s to km/h for the history plot
            speedHistory.add(Pair(ds.rawSpeed * 3.6f, ds.fusedSpeed * 3.6f))
            if (speedHistory.size > 80) {
                speedHistory.removeAt(0)
            }
        }
    }

    val state = daemonState
    val rawSpeed = state?.rawSpeed ?: 0f
    val fusedSpeed = state?.fusedSpeed ?: 0f
    val rpm = state?.rpm ?: 800f
    val steeringAngle = state?.steeringAngle ?: 0f
    val brake = state?.brake ?: 0f
    val safetyScore = state?.safetyScore ?: 0f
    val ruleText = state?.ruleText ?: "Initializing Native Daemon..."
    val confidence = state?.confidence ?: 1f
    val acceleration = state?.acceleration ?: 0f

    val (statusColor, statusText) = when {
        safetyScore >= 60f -> Color(0xFFFF3B5C) to "CRITICAL ALERT"
        safetyScore >= 25f -> Color(0xFFFFB020) to "WARNING ALERT"
        else -> Color(0xFF00E676) to "SAFE DRIVING"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070A12), Color(0xFF0C1220))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // LEFT COLUMN: Controls, Status & Gauges
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // XAI Driver Status Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF131C30))
                        .border(1.dp, Color(0xFF22304D), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "DRIVER SAFETY SCORE: ${"%.0f".format(100f - safetyScore)}/100",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = ruleText,
                            color = Color(0xFFA9B6CF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { confidence },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF00D9FF),
                            trackColor = Color(0xFF22304D)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Inference Confidence: ${(confidence * 100).toInt()}%",
                            color = Color(0xFF6D7B9B),
                            fontSize = 11.sp
                        )
                    }
                }

                // Gauges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RpmGauge(rpm = rpm)
                    InteractiveSteeringWheel(angle = steeringAngle)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Navigation button
                Button(
                    onClick = onHistoryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2E4D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        "VIEW TRIP HISTORY",
                        color = Color(0xFF00D9FF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // RIGHT COLUMN: Canvas Speed Chart & Telemetry Data
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Real-time Canvas Line Chart
                SpeedComparisonChart(
                    history = speedHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f)
                )

                // Grid of Telemetry Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardSensorCard(
                        title = "RAW SPEED",
                        value = "${"%.1f".format(rawSpeed * 3.6f)}",
                        unit = "km/h",
                        color = Color(0xFFFF3B5C),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardSensorCard(
                        title = "FUSED SPEED",
                        value = "${"%.1f".format(fusedSpeed * 3.6f)}",
                        unit = "km/h",
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardSensorCard(
                        title = "ACCELERATION",
                        value = "${"%.2f".format(acceleration)}",
                        unit = "m/s²",
                        color = Color(0xFF00D9FF),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardSensorCard(
                        title = "BRAKE FORCE",
                        value = "${(brake * 100).toInt()}",
                        unit = "%",
                        color = Color(0xFFFFB020),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, color = Color(0xFFA9B6CF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpeedComparisonChart(history: List<Pair<Float, Float>>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF131C30))
            .border(1.dp, Color(0xFF22304D), RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sensor Fusion Latency & Jitter Monitor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem("Raw Signal", Color(0xFFFF3B5C))
                    LegendItem("Fused Signal", Color(0xFF00E676))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier
                .fillMaxSize()
                .weight(1f)) {
                val width = size.width
                val height = size.height

                // Draw horizontal grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height * i / gridLines
                    drawLine(
                        color = Color(0xFF22304D).copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                if (history.size < 2) return@Canvas

                // Scale speed dynamically up to max in history (with floor of 60 km/h)
                val maxSpeed = (history.maxOfOrNull { maxOf(it.first, it.second) } ?: 60f)
                    .coerceAtLeast(60f)

                val rawPath = androidx.compose.ui.graphics.Path()
                val fusedPath = androidx.compose.ui.graphics.Path()

                history.forEachIndexed { index, pair ->
                    val x = width * index / (history.size - 1)
                    val yRaw = height - (pair.first / maxSpeed * height)
                    val yFused = height - (pair.second / maxSpeed * height)

                    if (index == 0) {
                        rawPath.moveTo(x, yRaw)
                        fusedPath.moveTo(x, yFused)
                    } else {
                        rawPath.lineTo(x, yRaw)
                        fusedPath.lineTo(x, yFused)
                    }
                }

                drawPath(
                    path = rawPath,
                    color = Color(0xFFFF3B5C),
                    style = Stroke(width = 3f)
                )

                drawPath(
                    path = fusedPath,
                    color = Color(0xFF00E676),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

@Composable
fun InteractiveSteeringWheel(angle: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(150.dp)
            .graphicsLayer(rotationZ = angle)
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 4.dp.toPx()

                // Outer steering wheel rim
                drawCircle(
                    color = Color(0xFF1A2238),
                    radius = radius,
                    style = Stroke(width = 14.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00D9FF),
                    radius = radius - 7.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inner Hub
                drawCircle(
                    color = Color(0xFF1A2238),
                    radius = radius * 0.28f
                )

                // Three steering spokes (left, right, bottom)
                // Left Spoke
                drawLine(
                    color = Color(0xFF1A2238),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x - radius * 0.28f, center.y),
                    strokeWidth = 12.dp.toPx()
                )
                // Right Spoke
                drawLine(
                    color = Color(0xFF1A2238),
                    start = Offset(center.x + radius * 0.28f, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 12.dp.toPx()
                )
                // Bottom Spoke
                drawLine(
                    color = Color(0xFF1A2238),
                    start = Offset(center.x, center.y + radius * 0.28f),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 12.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("ADAS", color = Color(0xFF00D9FF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
fun RpmGauge(rpm: Float, modifier: Modifier = Modifier) {
    val maxRpm = 8000f
    val sweptAngle = 240f
    val startAngle = 150f

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 6.dp.toPx()

            // Draw background dial arc
            drawArc(
                color = Color(0xFF22304D).copy(alpha = 0.6f),
                startAngle = startAngle,
                sweepAngle = sweptAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw active RPM arc
            val activeSweep = (rpm / maxRpm * sweptAngle).coerceIn(0f, sweptAngle)
            val color = if (rpm > 6000f) Color(0xFFFF3B5C) else if (rpm > 4500f) Color(0xFFFFB020) else Color(0xFF00D9FF)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${rpm.toInt()}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "RPM",
                color = Color(0xFF6D7B9B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun DashboardSensorCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131C30))
            .border(1.dp, Color(0xFF22304D), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF6D7B9B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 13.sp,
                    color = Color(0xFFA9B6CF),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}