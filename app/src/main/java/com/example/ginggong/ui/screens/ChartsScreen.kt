package com.example.ginggong.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ginggong.data.model.BodyLog
import com.example.ginggong.data.model.FoodLog
import com.example.ginggong.data.model.WaterLog

@Composable
fun ChartsScreen(
    waterLogs: List<WaterLog>,
    foodLogs: List<FoodLog>,
    bodyLogs: List<BodyLog>
) {
    var selectedFilter by remember { mutableStateOf("Weekly") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF0066FF)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            // Water Chart Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "💧 Water Intake ($selectedFilter)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val sampleWaterData = listOf(1800f, 2100f, 2500f, 2200f, 2700f, 2400f, 2800f)
                    val daysLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                    BarChartCanvas(
                        data = sampleWaterData,
                        labels = daysLabels,
                        maxVal = 3000f,
                        barColor = Color(0xFF0066FF)
                    )
                }
            }
        }

        item {
            // Calories & Macros Chart Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "🍚 Calories Consumed ($selectedFilter)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val sampleCalorieData = listOf(1950f, 2100f, 1850f, 2250f, 2000f, 2400f, 2150f)
                    val daysLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                    BarChartCanvas(
                        data = sampleCalorieData,
                        labels = daysLabels,
                        maxVal = 2500f,
                        barColor = Color(0xFFFF5E00)
                    )
                }
            }
        }

        item {
            // Weight & BMI Trend
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "⚖ Weight & BMI Trend ($selectedFilter)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val sampleWeightData = listOf(69.5f, 69.1f, 68.8f, 68.5f, 68.2f, 68.0f, 67.8f)
                    val daysLabels = listOf("W1", "W2", "W3", "W4", "W5", "W6", "W7")

                    LineChartCanvas(
                        data = sampleWeightData,
                        labels = daysLabels,
                        lineColor = Color(0xFF34C759)
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartCanvas(data: List<Float>, labels: List<String>, maxVal: Float, barColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2)

        data.forEachIndexed { index, value ->
            val barHeight = (value / maxVal) * (height - 30f)
            val left = (index * barWidth * 2) + (barWidth / 2)
            val top = height - barHeight - 20f

            drawRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun LineChartCanvas(data: List<Float>, labels: List<String>, lineColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val width = size.width
        val height = size.height
        val minVal = (data.minOrNull() ?: 60f) - 1f
        val maxVal = (data.maxOrNull() ?: 75f) + 1f

        val points = data.mapIndexed { index, valPoint ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = height - ((valPoint - minVal) / (maxVal - minVal)) * (height - 30f) - 15f
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f
            )
        }

        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 6f, center = pt)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
