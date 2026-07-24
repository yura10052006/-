package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MonthSummary
import java.time.YearMonth
import java.util.Locale

@Composable
fun WorkerScreen(
    viewModel: MainViewModel,
    onShowToast: (String) -> Unit
) {
    val currentTab = remember { mutableStateOf(0) } // 0: Календар, 1: Історія, 2: Налаштування

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C10))
    ) {
        // Sticky Subnavigation Tabs
        TabRow(
            selectedTabIndex = currentTab.value,
            containerColor = Color(0xFF14141C),
            contentColor = Color(0xFF8B78FF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab.value]),
                    color = Color(0xFF8B78FF)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = currentTab.value == 0,
                onClick = { currentTab.value = 0 },
                text = { Text("Календар", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Календар") }
            )
            Tab(
                selected = currentTab.value == 1,
                onClick = { currentTab.value = 1 },
                text = { Text("Історія", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                icon = { Icon(Icons.Default.History, contentDescription = "Історія") }
            )
            Tab(
                selected = currentTab.value == 2,
                onClick = { currentTab.value = 2 },
                text = { Text("Налаштування", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Налаштування") }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (currentTab.value) {
                0 -> CalendarAndResultTab(viewModel, onShowToast)
                1 -> HistoryTab(viewModel)
                2 -> SettingsTab(viewModel, onShowToast)
            }
        }
    }
}

@Composable
fun CalendarAndResultTab(
    viewModel: MainViewModel,
    onShowToast: (String) -> Unit
) {
    val user by viewModel.currentUserModel.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isSaved by viewModel.isMonthSaved.collectAsState()
    val isPickerOpen = remember { mutableStateOf(false) }

    val calc = viewModel.currentCalculation()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Month Selector Card
            MonthSelectorCard(
                selectedMonth = selectedMonth,
                isPickerOpen = isPickerOpen,
                savedMonthsKeys = user?.months?.keys ?: emptySet()
            ) { newMonth ->
                viewModel.selectMonth(newMonth)
                isPickerOpen.value = false
            }
        }

        item {
            // Legend selection row
            user?.scheme?.let { scheme ->
                LegendSelector(
                    dayTypes = scheme.dayTypes,
                    activeType = viewModel.activeLegendType.collectAsState().value,
                    onSelect = { viewModel.selectLegendType(it) }
                )
            }
        }

        item {
            // Calendar Grid Card
            CalendarGridCard(
                selectedMonth = selectedMonth,
                dayData = viewModel.currentMonthDayData.collectAsState().value,
                scheme = user?.scheme ?: viewModel.getDefaultScheme(),
                onDayClick = { day ->
                    viewModel.setDayType(day)
                }
            )
        }

        item {
            // Save Month Button with saved status label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSaved) Color(0xFF34D47A) else Color(0xFFF05C52))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSaved) "✓ Збережено" else "Незбережені зміни",
                        color = if (isSaved) Color(0xFF34D47A) else Color(0xFFF05C52),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveCurrentMonth()
                        onShowToast("Успішно збережено в історію!")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color(0xFF2A2A3E) else Color(0xFF8B78FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Зберегти")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Зберегти місяць")
                }
            }
        }

        item {
            // Result Block
            ResultCard(summary = calc)
        }
    }
}

@Composable
fun MonthSelectorCard(
    selectedMonth: String,
    isPickerOpen: MutableState<Boolean>,
    savedMonthsKeys: Set<String>,
    onMonthSelected: (String) -> Unit
) {
    val parts = selectedMonth.split("-")
    val year = parts[0]
    val monthIndex = parts[1].toInt()
    val ukrMonth = getUkrainianMonthName(monthIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPickerOpen.value = !isPickerOpen.value },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Обраний місяць розрахунку", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = "$ukrMonth $year",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isPickerOpen.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Вибір місяця",
                    tint = Color(0xFF8B78FF)
                )
            }

            if (isPickerOpen.value) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF2A2A3E))
                Spacer(modifier = Modifier.height(12.dp))

                // Year navigation simple
                var activeYear by remember { mutableStateOf(year.toInt()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Попередній рік", tint = Color.White)
                    }
                    Text(text = activeYear.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { activeYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Наступний рік", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4x3 Month selector grid
                val monthsList = listOf(
                    "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                    "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val mIdx = row * 3 + col + 1
                                val mKey = String.format(Locale.US, "%d-%02d", activeYear, mIdx)
                                val label = monthsList[mIdx - 1]
                                val isSavedMonth = savedMonthsKeys.contains(mKey)
                                val isSelected = selectedMonth == mKey

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) Color(0xFF8B78FF) else Color(0xFF1E1E2C),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else Color(0xFF2A2A3E),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onMonthSelected(mKey) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = label.take(4) + ".",
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSavedMonth && !isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34D47A))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendSelector(
    dayTypes: List<DayType>,
    activeType: DayType?,
    onSelect: (DayType) -> Unit
) {
    Column {
        Text("Оберіть активний статус для заповнення", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScrollableContainer(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayTypes.filter { !it.weekend }.forEach { type ->
                val isSelected = activeType?.id == type.id
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color(0xFF1E1E2C) else Color(0xFF14141C),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF8B78FF) else Color(0xFF2A2A3E),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(type) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(type.color))
                        )
                        Text(
                            text = type.name.split(" ").firstOrNull() ?: type.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper to allow scrolling row of Legend items
@Composable
fun Modifier.horizontalScrollableContainer(): Modifier {
    // Standard horizontal arrangement scroll fallback for small screens
    return this.wrapContentWidth()
}

@Composable
fun CalendarGridCard(
    selectedMonth: String,
    dayData: Map<String, String>,
    scheme: SalaryScheme,
    onDayClick: (Int) -> Unit
) {
    val parts = selectedMonth.split("-")
    val year = parts[0].toInt()
    val month = parts[1].toInt()

    val ym = YearMonth.of(year, month)
    val daysInMonth = ym.lengthOfMonth()
    val firstDayOfWeek = ym.atDay(1).dayOfWeek.value // 1-Mon..7-Sun

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Days of week
            val ukrDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
            Row(modifier = Modifier.fillMaxWidth()) {
                ukrDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar weeks calculation
            val totalCells = daysInMonth + (firstDayOfWeek - 1)
            val rows = (totalCells + 6) / 7

            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val cellIdx = r * 7 + c + 1
                        val dayNumber = cellIdx - (firstDayOfWeek - 1)

                        if (dayNumber in 1..daysInMonth) {
                            val dayOfWeek = ym.atDay(dayNumber).dayOfWeek.value
                            val dayKey = dayNumber.toString()
                            val assignedTypeId = dayData[dayKey]
                            val assignedType = scheme.dayTypes.find { it.id == assignedTypeId }

                            // Check Sunday / Saturday
                            val isSunday = dayOfWeek == 7
                            val isSaturday = dayOfWeek == 6

                            val isWorkingSaturday = isSaturday && assignedTypeId == "saturday"

                            // Background calculation
                            val bg = when {
                                isSunday -> Color(0xFF1E1B24) // Sunday inert dark container
                                isWorkingSaturday -> parseHexColor("#f07840") // saturday worked (weekend orange)
                                isSaturday -> Color(0xFF1B1B22) // rest Saturday
                                assignedType != null -> parseHexColor(assignedType.color)
                                else -> Color(0xFF1E1E2C) // weekday rest/unassigned
                            }

                            val txtColor = when {
                                isWorkingSaturday -> Color.White
                                isSunday -> Color.DarkGray
                                isSaturday -> Color.Gray
                                assignedType != null -> Color.White
                                else -> Color.LightGray
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .background(bg, shape = RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isSunday) {
                                        onDayClick(dayNumber)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNumber.toString(),
                                        color = txtColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    // Visual badge or text
                                    if (isWorkingSaturday) {
                                        Text("+1.8k", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    } else if (assignedType != null && assignedType.bonus > 0 && !isSaturday) {
                                        Text("+${assignedType.bonus.toInt()}", color = Color.White, fontSize = 8.sp)
                                    }
                                }
                            }
                        } else {
                            // Blank filler box
                            Box(modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFF2A2A3E))
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle hints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1B1B22)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Суб (вихідна)", color = Color.Gray, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF07840)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Робоча Суб (+1800₴)", color = Color.Gray, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1E1B24)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Неділя (заблоковано)", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ResultCard(summary: MonthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ОЧІКУВАНА ЗАРПЛАТА", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))

            // Large gradient sum
            Text(
                text = String.format(Locale.US, "%,.0f ₴", summary.total),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF8B78FF), Color(0xFF34D47A))
                    )
                )
            )

            Spacer(modifier = Modifier.height(18.dp))
            Divider(color = Color(0xFF2A2A3E))
            Spacer(modifier = Modifier.height(18.dp))

            // 2x3 Grid breakdown
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1
                Row(modifier = Modifier.fillMaxWidth()) {
                    BreakdownItem(modifier = Modifier.weight(1f), label = "База", value = String.format(Locale.US, "%,.0f ₴", summary.basePay), iconColor = Color(0xFF8B78FF))
                    BreakdownItem(modifier = Modifier.weight(1f), label = "Премія", value = String.format(Locale.US, "%,.0f ₴", summary.premium), iconColor = Color(0xFF34D47A))
                }
                // Row 2
                Row(modifier = Modifier.fillMaxWidth()) {
                    BreakdownItem(modifier = Modifier.weight(1f), label = "Бонуси", value = String.format(Locale.US, "%,.0f ₴", summary.bonus), iconColor = Color(0xFFF07840))
                    BreakdownItem(modifier = Modifier.weight(1f), label = "Відрахування", value = String.format(Locale.US, "-%,.0f ₴", summary.deductions), iconColor = Color(0xFFF05C52))
                }
                // Row 3
                Row(modifier = Modifier.fillMaxWidth()) {
                    BreakdownItem(modifier = Modifier.weight(1f), label = "Відпрацьовано", value = "${summary.worked} / ${summary.workDays} дн", iconColor = Color.Cyan)
                    BreakdownItem(modifier = Modifier.weight(1f), label = "Лікарняних", value = "${summary.sick} дн", iconColor = Color(0xFFF0C040))
                }
            }
        }
    }
}

@Composable
fun BreakdownItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = iconColor, radius = size.minDimension / 2)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, color = Color.Gray, fontSize = 11.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryTab(viewModel: MainViewModel) {
    val user by viewModel.currentUserModel.collectAsState()

    val historyList = remember(user) {
        user?.months?.entries?.sortedByDescending { it.key } ?: emptyList()
    }

    if (historyList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = "Порожньо", tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Історія порожня. Збережіть поточний місяць!", color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(historyList) { (monthKey, saved) ->
                val parts = monthKey.split("-")
                val yr = parts[0]
                val mt = parts[1].toInt()
                val monthName = getUkrainianMonthName(mt)

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column {
                            Text(
                                text = "$monthName $yr",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Робота: ${saved.worked}/${saved.workDays} дн",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                                if (saved.sick > 0) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Лікарняні: ${saved.sick} дн",
                                        color = Color(0xFFF0C040),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = String.format(Locale.US, "%,.0f ₴", saved.total),
                            color = Color(0xFF8B78FF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    onShowToast: (String) -> Unit
) {
    val user by viewModel.currentUserModel.collectAsState()

    var baseRate by remember(user) { mutableStateOf(user?.scheme?.base?.toString() ?: "28000") }
    var premiumRate by remember(user) { mutableStateOf(user?.scheme?.premium?.toString() ?: "2000") }
    var sickRate by remember(user) { mutableStateOf(user?.scheme?.sickRate?.toString() ?: "60") }
    var displayName by remember(user) { mutableStateOf(user?.displayName ?: "") }

    // Change Password inputs
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Day types editable copy
    var dayTypesList by remember(user) {
        mutableStateOf(user?.scheme?.dayTypes ?: emptyList())
    }

    // Modal adding dayType
    var isAddTypeOpen by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }
    var newTypeBonus by remember { mutableStateOf("") }
    var newTypeColor by remember { mutableStateOf("#8B78FF") }

    // Default weekday setup simple values Mon, Tue, etc.
    // Monday/Wednesday/Friday default day type ID, Tuesday/Thursday default day type ID
    var monWedFriType by remember(user) {
        mutableStateOf(user?.scheme?.defaultWeekday?.get("1") ?: "gym")
    }
    var tueThuType by remember(user) {
        mutableStateOf(user?.scheme?.defaultWeekday?.get("2") ?: "full-bonus")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Параметри розрахунку ставки", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Ім'я / Посада", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B78FF),
                            unfocusedBorderColor = Color(0xFF2A2A3E),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = baseRate,
                            onValueChange = { baseRate = it },
                            label = { Text("База (₴)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            value = premiumRate,
                            onValueChange = { premiumRate = it },
                            label = { Text("Премія (₴)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                    OutlinedTextField(
                        value = sickRate,
                        onValueChange = { sickRate = it },
                        label = { Text("Відсоток лікарняних (%)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B78FF),
                            unfocusedBorderColor = Color(0xFF2A2A3E),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        item {
            Text("Дефолтний розклад тижня", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Розклад за умовчанням:", color = Color.Gray, fontSize = 12.sp)

                    // Mon/Wed/Fri Selection Dropdown Simulation via simple Row click
                    Column {
                        Text("Пн/Ср/Пт (будні)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dayTypesList.filter { !it.weekend }.forEach { type ->
                                val active = monWedFriType == type.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (active) Color(0xFF8B78FF) else Color(0xFF1E1E2C), shape = RoundedCornerShape(8.dp))
                                        .clickable { monWedFriType = type.id }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(type.name.split(" ").firstOrNull() ?: type.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Tue/Thu Choice
                    Column {
                        Text("Вт/Чт (будні)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dayTypesList.filter { !it.weekend }.forEach { type ->
                                val active = tueThuType == type.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (active) Color(0xFF8B78FF) else Color(0xFF1E1E2C), shape = RoundedCornerShape(8.dp))
                                        .clickable { tueThuType = type.id }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(type.name.split(" ").firstOrNull() ?: type.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Список типів днів", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isAddTypeOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Додати", tint = Color(0xFF34D47A))
                }
            }
        }

        // List Day Types inline
        items(dayTypesList) { type ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(type.color))
                        )
                        Column {
                            Text(type.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            val bonusLab = if (type.bonus > 0) "+${type.bonus.toInt()} ₴" else "Без бонусу"
                            var flagsStr = ""
                            if (type.deduct) flagsStr += " [Відрахування]"
                            if (type.sick) flagsStr += " [Лікарняний]"
                            Text("$bonusLab$flagsStr", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    // Forbid deleting core system days like absent, sick, saturday
                    val isSystemDay = type.id == "absent" || type.id == "sick" || type.id == "saturday" || type.id == "gym" || type.id == "full-bonus"
                    if (!isSystemDay) {
                        IconButton(onClick = {
                            dayTypesList = dayTypesList.filter { it.id != type.id }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = Color(0xFFF05C52))
                        }
                    }
                }
            }
        }

        item {
            Text("Оновити пароль доступу", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                border = BorderStroke(1.dp, Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Новий пароль", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B78FF),
                            unfocusedBorderColor = Color(0xFF2A2A3E),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Підтвердити новий пароль", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B78FF),
                            unfocusedBorderColor = Color(0xFF2A2A3E),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val br = baseRate.toDoubleOrNull()
                    val pr = premiumRate.toDoubleOrNull()
                    val sr = sickRate.toDoubleOrNull()

                    if (br == null || pr == null || sr == null) {
                        onShowToast("Будь ласка, введіть коректні числа")
                        return@Button
                    }
                    if (displayName.isBlank()) {
                        onShowToast("Введіть ім'я працівника")
                        return@Button
                    }

                    var passUpdate: String? = null
                    if (newPassword.isNotBlank()) {
                        if (newPassword != confirmPassword) {
                            onShowToast("Паролі не співпадають!")
                            return@Button
                        }
                        if (newPassword.length < 4) {
                            onShowToast("Пароль має бути не менше 4 символів!")
                            return@Button
                        }
                        passUpdate = newPassword
                    }

                    // Build weekday map
                    val updatedWeekMap = mapOf(
                        "1" to monWedFriType,
                        "2" to tueThuType,
                        "3" to monWedFriType,
                        "4" to tueThuType,
                        "5" to monWedFriType
                    )

                    val statusMsg = viewModel.saveSettings(
                        base = br,
                        premium = pr,
                        sickRate = sr,
                        displayName = displayName,
                        passwordChange = passUpdate,
                        dayTypes = dayTypesList,
                        defaultWeekMap = updatedWeekMap
                    )

                    onShowToast(statusMsg)
                    newPassword = ""
                    confirmPassword = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B78FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Зберегти")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Зберегти всі налаштування", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal view for adding customized day types
    if (isAddTypeOpen) {
        AlertDialog(
            onDismissRequest = { isAddTypeOpen = false },
            containerColor = Color(0xFF14141C),
            title = { Text("Додати новий тип дня", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = { newTypeName = it },
                        label = { Text("Назва статусу") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF8B78FF), unfocusedBorderColor = Color(0xFF2A2A3E))
                    )
                    OutlinedTextField(
                        value = newTypeBonus,
                        onValueChange = { newTypeBonus = it },
                        label = { Text("Бонус в гривнях (₴)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF8B78FF), unfocusedBorderColor = Color(0xFF2A2A3E))
                    )

                    Text("Оберіть колір статусу:", color = Color.Gray, fontSize = 12.sp)

                    val colorsPreset = listOf("#8b78ff", "#34d47a", "#00bcd4", "#e91e63", "#9c27b0", "#ffeb3b", "#ff9800")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorsPreset.forEach { col ->
                            val isSelected = newTypeColor.equals(col, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(col)))
                                    .border(width = if (isSelected) 2.dp else 0.dp, color = Color.White, shape = CircleShape)
                                    .clickable { newTypeColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bn = newTypeBonus.toDoubleOrNull() ?: 0.0
                        if (newTypeName.isBlank()) return@Button

                        val cleanId = "custom-" + newTypeName.lowercase().replace(" ", "-")
                        val newType = DayType(
                            id = cleanId,
                            name = newTypeName,
                            bonus = bn,
                            color = newTypeColor
                        )

                        dayTypesList = dayTypesList + newType
                        isAddTypeOpen = false
                        newTypeName = ""
                        newTypeBonus = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B78FF))
                ) {
                    Text("Додати")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddTypeOpen = false }) {
                    Text("Скасувати", color = Color.Gray)
                }
            }
        )
    }
}

// Global Ukrainian month localization lookups
fun getUkrainianMonthName(idx: Int): String {
    return when (idx) {
        1 -> "Січень"
        2 -> "Лютий"
        3 -> "Березень"
        4 -> "Квітень"
        5 -> "Травено" // standard layout ukr
        5 -> "Травень"
        6 -> "Червень"
        7 -> "Липень"
        8 -> "Серпень"
        9 -> "Вересень"
        10 -> "Жовтень"
        11 -> "Листопад"
        12 -> "Грудень"
        else -> "Місяць"
    }
}

fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        val cleanHex = hex.trim().replace("#", "")
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
