package com.smartcabin.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmartCabinDashboard(viewModel: SmartDriveViewModel, onHistoryClick: () -> Unit) {
    // Collecting StateFlow safely
    val daemonState by viewModel.daemonState.collectAsState()
    val isConnected = daemonState != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        HeaderSection(isConnected)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: XAI and Warnings
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                XaiDecisionPanel(
                    safetyScore = daemonState?.safetyScore?.toInt() ?: 100,
                    dominantRule = daemonState?.ruleText ?: "System Ready"
                )
                
                IndicatorsPanel(
                    steeringAngle = daemonState?.steeringAngle ?: 0f,
                    rpm = daemonState?.rpm ?: 0f
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = onHistoryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2E4D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "VIEW TRIP HISTORY",
                        color = Color(0xFF00D9FF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Right Panel: Speed Comparison Chart (Raw vs Fused)
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(Color(0xFF131A2A), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sensor Fusion Pipeline (Canvas)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                SpeedComparisonChart(
                    rawSpeed = daemonState?.rawSpeed ?: 0f,
                    fusedSpeed = daemonState?.fusedSpeed ?: 0f,
                    modifier = Modifier.fillMaxSize().padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderSection(isConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SMARTCABIN XAI DASHBOARD",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isConnected) Color.Green else Color.Red, RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "DAEMON CONNECTED" else "WAITING FOR DAEMON",
                color = if (isConnected) Color.Green else Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun XaiDecisionPanel(safetyScore: Int, dominantRule: String) {
    // We get a safetyScore (0 to 100), where 100 is safe and 0 is critical.
    val risk = 100 - safetyScore
    val (panelColor, statusText) = when {
        risk > 75 -> Color(0xFFFF3B5C) to "CRITICAL ALERT"
        risk > 40 -> Color(0xFFFFB020) to "WARNING"
        else -> Color(0xFF00E676) to "SAFE"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = panelColor.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "DRIVER STATE INFERENCE",
                color = panelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = statusText,
                color = panelColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DOMINANT RULE:",
                color = Color.LightGray,
                fontSize = 10.sp
            )
            Text(
                text = dominantRule,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun IndicatorsPanel(steeringAngle: Float, rpm: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        RpmGauge(rpm = rpm)
        InteractiveSteeringWheel(angle = steeringAngle)
    }
}

@Composable
fun SpeedComparisonChart(
    rawSpeed: Float,
    fusedSpeed: Float,
    modifier: Modifier = Modifier,
    maxPoints: Int = 100
) {
    val rawHistory = remember { mutableStateListOf<Float>() }
    val fusedHistory = remember { mutableStateListOf<Float>() }

    LaunchedEffect(rawSpeed, fusedSpeed) {
        rawHistory.add(rawSpeed)
        fusedHistory.add(fusedSpeed)
        
        if (rawHistory.size > maxPoints) {
            rawHistory.removeFirst()
            fusedHistory.removeFirst()
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        val width = size.width
        val height = size.height
        
        val maxSpeedDisplay = 160f 
        val pointSpacing = width / (maxPoints - 1).coerceAtLeast(1).toFloat()

        val gridColor = Color(0xFF22304D)
        for (i in 0..4) {
            val y = height - (height * (i * 40f / maxSpeedDisplay))
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        fun drawSpeedPath(history: List<Float>, pathColor: Color, isSmooth: Boolean = false) {
            if (history.isEmpty()) return

            val path = Path()
            history.forEachIndexed { index, speed ->
                val x = index * pointSpacing
                val y = height - ((speed / maxSpeedDisplay) * height).coerceIn(0f, height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = if (isSmooth) 6f else 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                alpha = if (isSmooth) 1.0f else 0.5f
            )
        }

        drawSpeedPath(
            history = rawHistory,
            pathColor = Color(0xFFFF3B5C),
            isSmooth = false
        )

        drawSpeedPath(
            history = fusedHistory,
            pathColor = Color(0xFF00E676),
            isSmooth = true
        )
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
