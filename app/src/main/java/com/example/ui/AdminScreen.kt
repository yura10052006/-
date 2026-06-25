package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    onShowToast: (String) -> Unit
) {
    val database by viewModel.database.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    // Form inputs for adding worker
    var wUsername by remember { mutableStateOf("") }
    var wPassword by remember { mutableStateOf("") }
    var wDisplayName by remember { mutableStateOf("") }

    // Dialog details
    var selectedWorkerForDialog by remember { mutableStateOf<Pair<String, UserModel>?>(null) }

    // Get list of workers only
    val workersList = remember(database) {
        database.users.filter { it.value.role == "worker" }
    }

    // Top statistical calculations for the selected month
    val workersCount = workersList.size

    val activeMonthTotals = remember(workersList, selectedMonth) {
        workersList.map { (key, worker) ->
            worker.months[selectedMonth]?.total ?: 0.0
        }
    }

    val totalSalaryBudget = remember(activeMonthTotals) {
        activeMonthTotals.sum()
    }

    val averageSalary = remember(activeMonthTotals, workersCount) {
        if (workersCount > 0) totalSalaryBudget / workersCount else 0.0
    }

    // Dynamic Multi-Month Wage Budget aggregation for Bar Chart
    val chartData = remember(database) {
        val allRecordMonths = mutableSetOf<String>()
        database.users.values.forEach { u ->
            if (u.role == "worker") {
                allRecordMonths.addAll(u.months.keys)
            }
        }
        val sortedMonths = allRecordMonths.sorted()
        sortedMonths.map { mKey ->
            val sum = database.users.values.filter { it.role == "worker" }.sumOf { u ->
                u.months[mKey]?.total ?: 0.0
            }
            mKey to sum
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Аналітика за місяць: $selectedMonth",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Admin Top Stats: 3 blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Працівники",
                    value = workersCount.toString(),
                    color = Color(0xFF8B78FF),
                    subText = "зареєстровано"
                )
                AdminStatCard(
                    modifier = Modifier.weight(1.2f),
                    label = "Середня ЗП",
                    value = String.format(Locale.US, "%,.0f ₴", averageSalary),
                    color = Color(0xFF34D47A),
                    subText = "за цей місяць"
                )
                AdminStatCard(
                    modifier = Modifier.weight(1.2f),
                    label = "Фонд ЗП",
                    value = String.format(Locale.US, "%,.0f ₴", totalSalaryBudget),
                    color = Color(0xFFF07840),
                    subText = "загальний витрата"
                )
            }
        }

        // SALARY BUDGET PROGRESS BAR CHART
        item {
            Text(
                text = "Розподіл фонду заробітних плат по місяцях",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (chartData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Дані за місяці поки відсутні в історії", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        SalaryBudgetBarChart(dataPoints = chartData)
                    }
                }
            }
        }

        item {
            Text(
                text = "Додати нового працівника у команду",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // FORM ADD WORKER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = wDisplayName,
                        onValueChange = { wDisplayName = it },
                        label = { Text("Ім'я та Прізвище", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B78FF),
                            unfocusedBorderColor = Color(0xFF2A2A3E),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = wUsername,
                            onValueChange = { wUsername = it },
                            label = { Text("Логін", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B78FF),
                                unfocusedBorderColor = Color(0xFF2A2A3E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = wPassword,
                            onValueChange = { wPassword = it },
                            label = { Text("Пароль", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B78FF),
                                unfocusedBorderColor = Color(0xFF2A2A3E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val err = viewModel.addWorker(wUsername, wPassword, wDisplayName)
                            if (err != null) {
                                onShowToast(err)
                            } else {
                                onShowToast("Працівника ${wDisplayName} успішно додано!")
                                wUsername = ""
                                wPassword = ""
                                wDisplayName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D47A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Додати")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Додати до списку працівників", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Список зареєстрованих працівників",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // WORKERS LIST INSIDE RECIPES
        if (workersList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Працівники відсутні. Додайте першого працівника вище!", color = Color.Gray)
                }
            }
        } else {
            items(workersList.entries.toList()) { (key, uModel) ->
                val monthData = uModel.months[selectedMonth]
                val currentMonthPay = monthData?.total ?: 0.0
                val sampleWorked = monthData?.worked ?: 0
                val sampleWorkDays = monthData?.workDays ?: 0
                val sampleSick = monthData?.sick ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedWorkerForDialog = key to uModel },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uModel.displayName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Логін: $key", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Календар ($selectedMonth): $sampleWorked/$sampleWorkDays дн",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                                if (sampleSick > 0) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "Лікарн: $sampleSick", color = Color(0xFFF0C040), fontSize = 11.sp)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%,.0f ₴", currentMonthPay),
                                color = Color(0xFF8B78FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("ЗП за $selectedMonth", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal dialog showing full worker history and account delete action
    selectedWorkerForDialog?.let { (username, uModel) ->
        AlertDialog(
            onDismissRequest = { selectedWorkerForDialog = null },
            containerColor = Color(0xFF14141C),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = uModel.displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Доступ: $username", color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            val deleted = viewModel.deleteWorker(username)
                            if (deleted) {
                                onShowToast("Працівника $username успішно видалено")
                                selectedWorkerForDialog = null
                            } else {
                                onShowToast("Неможливо видалити адміна")
                            }
                        }
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Видалити працівника", tint = Color(0xFFF05C52))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    Text(
                        text = "Історія місяців:",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val workerHistory = uModel.months.entries.sortedByDescending { it.key }
                    if (workerHistory.isEmpty()) {
                        Text(
                            text = "Користувач ще не зберіг жодного місячного розрахунку з календаря.",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(workerHistory) { (monthKey, saved) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = monthKey, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Роб: ${saved.worked}/${saved.workDays} дн | Лік: ${saved.sick} дн",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%,.0f ₴", saved.total),
                                        color = Color(0xFF34D47A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Divider(color = Color(0xFF2A2A3E))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedWorkerForDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B78FF))
                ) {
                    Text("Закрити")
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subText: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subText, color = Color.DarkGray, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
fun SalaryBudgetBarChart(dataPoints: List<Pair<String, Double>>) {
    // Canvas dimensions and ratios
    val maxVal = dataPoints.maxOfOrNull { it.second } ?: 1.0
    // Rounded dynamic ceiling mapping
    val mapMax = if (maxVal > 0) maxVal * 1.15 else 1000.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 16.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // X offsets, graph height mapping
            val spacing = 32.dp.toPx()
            val availableWidth = canvasWidth - spacing
            val barCount = dataPoints.size
            val barWidth = if (barCount > 0) (availableWidth / barCount) - spacing else availableWidth

            // Main axes draw
            drawLine(
                color = Color(0xFF2A2A3E),
                start = Offset(spacing, canvasHeight - 20.dp.toPx()),
                end = Offset(canvasWidth, canvasHeight - 20.dp.toPx()),
                strokeWidth = 2f
            )

            // Gridlines helper metrics
            val stepLines = 3
            for (i in 1..stepLines) {
                val gridY = (canvasHeight - 20.dp.toPx()) * i / (stepLines + 1)
                drawLine(
                    color = Color(0xFF1F1F2E),
                    start = Offset(spacing, gridY),
                    end = Offset(canvasWidth, gridY),
                    strokeWidth = 1f
                )
            }

            dataPoints.forEachIndexed { idx, (monthKey, baseVal) ->
                val x = spacing + idx * (barWidth + spacing)
                val activeH = ((baseVal / mapMax) * (canvasHeight - 40.dp.toPx())).toFloat()
                val finalY = canvasHeight - 20.dp.toPx() - activeH

                // Draw Bar with beautiful Cyber Purple & Green gradient
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B78FF), Color(0xFF34D47A))
                )

                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(x, finalY),
                    size = Size(barWidth, activeH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Label below: month of month text
                // Transform e.g. "2026-06" to simple month index "06"
                val monthLabel = monthKey.split("-").getOrNull(1) ?: monthKey

                // Standard canvas Native Paint text rendering (safest for direct typography in charts)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.dp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 9.dp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    // Month label at bottom ticks
                    canvas.nativeCanvas.drawText(
                        monthLabel,
                        x + barWidth / 2,
                        canvasHeight - 4.dp.toPx(),
                        paint
                    )

                    // Sum on top of bars
                    val printableSum = if (baseVal >= 1000) {
                        String.format(Locale.US, "%.1fk", baseVal / 1000)
                    } else {
                        baseVal.toInt().toString()
                    }

                    canvas.nativeCanvas.drawText(
                        printableSum,
                        x + barWidth / 2,
                        finalY - 6.dp.toPx(),
                        labelPaint
                    )
                }
            }
        }
    }
}
