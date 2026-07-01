package com.smartcabin.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcabin.domain.model.Trip
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: TripHistoryViewModel = hiltViewModel()
) {
    val trips by viewModel.allTrips.collectAsState()

    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    
    LaunchedEffect(trips) {
        if (trips.isNotEmpty()) {
            val entries = trips.reversed().mapIndexed { index, trip ->
                FloatEntry(x = index.toFloat(), y = trip.finalScore.toFloat())
            }
            chartEntryModelProducer.setEntries(entries)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541))
        )
    )) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TRIP HISTORY", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Button(
                    onClick = onBackClick, 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A506B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("BACK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (trips.size > 1) {
                Text("DRIVER SCORE TREND", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                ) {
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(
                            label = textComponent(color = Color.White.copy(alpha = 0.5f)),
                            axis = lineComponent(color = Color.White.copy(alpha = 0.2f))
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = textComponent(color = Color.White.copy(alpha = 0.5f)),
                            axis = lineComponent(color = Color.White.copy(alpha = 0.2f))
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("RECENT TRIPS", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (trips.isEmpty()) {
                Text("No trips recorded yet.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.5f))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(trips) { trip ->
                        TripCard(trip)
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(trip.startTime))
    val durationMin = if (trip.endTime != null) ((trip.endTime - trip.startTime) / 60000).toInt() else 0

    val scoreColor = when {
        trip.finalScore >= 80 -> Color(0xFF00E676)
        trip.finalScore >= 50 -> Color(0xFFFF9100)
        else -> Color(0xFFFF1744)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = dateString, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dur: $durationMin min  •  Avg Spd: ${"%.0f".format(trip.avgSpeed * 3.6f)} km/h", 
                    fontSize = 14.sp, 
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "SCORE", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(text = "${trip.finalScore}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = scoreColor)
            }
        }
    }
}
