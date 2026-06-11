package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SalaryRepository(application)

    // All database state
    private val _database = MutableStateFlow(repository.loadDatabase())
    val database: StateFlow<SalaryDatabase> = _database.asStateFlow()

    // Authentication & Active User
    private val _activeUserKey = MutableStateFlow<String?>(null)
    val activeUserKey: StateFlow<String?> = _activeUserKey.asStateFlow()

    private val _currentUserModel = MutableStateFlow<UserModel?>(null)
    val currentUserModel: StateFlow<UserModel?> = _currentUserModel.asStateFlow()

    // Interactive Screen states
    private val _selectedMonth = MutableStateFlow("2026-06") // Default to June 2026 as current time
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _activeLegendType = MutableStateFlow<DayType?>(null)
    val activeLegendType: StateFlow<DayType?> = _activeLegendType.asStateFlow()

    // Temporary values for edited states (to trigger save banner with click)
    private val _currentMonthDayData = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentMonthDayData: StateFlow<Map<String, String>> = _currentMonthDayData.asStateFlow()

    private val _isMonthSaved = MutableStateFlow(true)
    val isMonthSaved: StateFlow<Boolean> = _isMonthSaved.asStateFlow()

    init {
        // Automatically default active legend type to standard full-bonus or first type
        val defaultScheme = repository.getDefaultScheme()
        _activeLegendType.value = defaultScheme.dayTypes.find { it.id == "full-bonus" } ?: defaultScheme.dayTypes.firstOrNull()
    }

    // AUTH ACTIONS
    fun login(username: String, pass: String): String? {
        val key = username.lowercase().trim()
        val db = _database.value
        val user = db.users[key]
        if (user != null && user.password == pass) {
            _activeUserKey.value = key
            _currentUserModel.value = user
            // Initialize month state
            val monthKey = _selectedMonth.value
            val savedMonth = user.months[monthKey]
            if (savedMonth != null) {
                _currentMonthDayData.value = savedMonth.dayData
                _isMonthSaved.value = true
            } else {
                // Initialize based on weekday templates
                val initializedData = initMonthWithTemplate(monthKey, user.scheme)
                _currentMonthDayData.value = initializedData
                _isMonthSaved.value = false
            }
            return null // Success
        }
        return "Невірний логін або пароль"
    }

    fun logout() {
        _activeUserKey.value = null
        _currentUserModel.value = null
    }

    // MONTH / CALENDAR ACTIONS
    fun selectMonth(monthKey: String) {
        _selectedMonth.value = monthKey
        val user = _currentUserModel.value ?: return
        val savedMonth = user.months[monthKey]
        if (savedMonth != null) {
            _currentMonthDayData.value = savedMonth.dayData
            _isMonthSaved.value = true
        } else {
            // Auto initialize with weekly schedule template
            val initializedData = initMonthWithTemplate(monthKey, user.scheme)
            _currentMonthDayData.value = initializedData
            _isMonthSaved.value = false
        }
    }

    fun selectLegendType(dayType: DayType) {
        _activeLegendType.value = dayType
    }

    // Day updates in calendar
    fun setDayType(dayNumber: Int) {
        val activeType = _activeLegendType.value ?: return
        // Block Sunday edits
        val parts = _selectedMonth.value.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val ym = YearMonth.of(year, month)
        val dow = ym.atDay(dayNumber).dayOfWeek.value // 1-Mon..7-Sun
        if (dow == 7) return // Sunday is uneditable

        val dayKey = dayNumber.toString()
        val currentData = _currentMonthDayData.value.toMutableMap()

        if (dow == 6) {
            // Saturday special behaviour: click toggles saturday on/off
            val isSaturdayCurrentlyWorked = currentData[dayKey] == "saturday"
            if (isSaturdayCurrentlyWorked) {
                currentData.remove(dayKey) // resets to standard rest Saturday
            } else {
                currentData[dayKey] = "saturday"
            }
        } else {
            // Regular weekday selection
            currentData[dayKey] = activeType.id
        }

        _currentMonthDayData.value = currentData
        _isMonthSaved.value = false
    }

    fun saveCurrentMonth() {
        val userKey = _activeUserKey.value ?: return
        val user = _currentUserModel.value ?: return
        val monthKey = _selectedMonth.value
        val scheme = user.scheme
        val dayData = _currentMonthDayData.value

        val parts = monthKey.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val computedMonth = performSalaryCalculation(year, month, dayData, scheme)

        val updatedMonths = user.months.toMutableMap()
        updatedMonths[monthKey] = computedMonth

        val updatedUser = user.copy(months = updatedMonths)
        repository.updateUser(userKey, updatedUser)

        // Sync local states
        _currentUserModel.value = updatedUser
        _database.value = repository.loadDatabase()
        _isMonthSaved.value = true
    }

    // SETTINGS ACTIONS
    fun saveSettings(
        base: Double,
        premium: Double,
        sickRate: Double,
        displayName: String,
        passwordChange: String?,
        dayTypes: List<DayType>,
        defaultWeekMap: Map<String, String>
    ): String {
        val userKey = _activeUserKey.value ?: return "Помилка автентифікації"
        val user = _currentUserModel.value ?: return "Помилка автентифікації"

        val updatedScheme = user.scheme.copy(
            base = base,
            premium = premium,
            sickRate = sickRate,
            dayTypes = dayTypes,
            defaultWeekday = defaultWeekMap
        )

        val newPassword = if (!passwordChange.isNullOrBlank()) passwordChange else user.password

        // Recompute all saved months based on the new scheme to keep records accurate!
        val updatedMonths = user.months.mapValues { (monthKey, saved) ->
            val parts = monthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            performSalaryCalculation(year, month, saved.dayData, updatedScheme)
        }

        val updatedUser = user.copy(
            displayName = displayName,
            password = newPassword,
            scheme = updatedScheme,
            months = updatedMonths
        )

        repository.updateUser(userKey, updatedUser)

        // Sync view states
        _currentUserModel.value = updatedUser
        _database.value = repository.loadDatabase()

        // Redo activeLegendType assignment if previous got deleted
        if (dayTypes.none { it.id == _activeLegendType.value?.id }) {
            _activeLegendType.value = dayTypes.find { it.id == "full-bonus" } ?: dayTypes.firstOrNull()
        }

        // Re-align current calendar calculation
        selectMonth(_selectedMonth.value)

        return "Налаштування успішно збережено!"
    }

    // ADMIN ACTIONS
    fun addWorker(username: String, pass: String, name: String): String? {
        val key = username.lowercase().trim()
        if (key.isBlank() || pass.isBlank() || name.isBlank()) {
            return "Заповніть всі поля працівника"
        }
        val defaultScheme = repository.getDefaultScheme()
        val newWorker = UserModel(
            password = pass,
            role = "worker",
            displayName = name,
            scheme = defaultScheme,
            months = emptyMap()
        )

        val success = repository.addUser(key, newWorker)
        if (success) {
            _database.value = repository.loadDatabase()
            return null // Success
        }
        return "Працівник з таким логіном вже існує"
    }

    fun deleteWorker(username: String): Boolean {
        val success = repository.deleteUser(username)
        if (success) {
            _database.value = repository.loadDatabase()
        }
        return success
    }

    // HELPER CALCULATION INTERFACE FOR ACTIVE USER
    fun currentCalculation(): MonthSummary {
        val user = _currentUserModel.value ?: return MonthSummary.EMPTY
        val parts = _selectedMonth.value.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        return getSummaryForData(year, month, _currentMonthDayData.value, user.scheme)
    }

    // CALENDAR ALGORITHMS
    private fun initMonthWithTemplate(monthKey: String, scheme: SalaryScheme): Map<String, String> {
        val parts = monthKey.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val ym = YearMonth.of(year, month)
        val daysInMonth = ym.lengthOfMonth()

        val map = mutableMapOf<String, String>()
        for (day in 1..daysInMonth) {
            val dow = ym.atDay(day).dayOfWeek.value // 1..7 (Mon..Sun)
            val dowStr = dow.toString()
            val templateType = scheme.defaultWeekday[dowStr]
            if (templateType != null && dow in 1..5) {
                map[day.toString()] = templateType
            }
        }
        return map
    }

    fun performSalaryCalculation(
        year: Int,
        month: Int,
        dayData: Map<String, String>,
        scheme: SalaryScheme
    ): SavedMonth {
        val sum = getSummaryForData(year, month, dayData, scheme)
        return SavedMonth(
            dayData = dayData,
            total = sum.total,
            worked = sum.worked,
            workDays = sum.workDays,
            sick = sum.sick,
            bonus = sum.bonus
        )
    }

    fun getSummaryForData(
        year: Int,
        month: Int,
        dayData: Map<String, String>,
        scheme: SalaryScheme
    ): MonthSummary {
        val ym = YearMonth.of(year, month)
        val daysInMonth = ym.lengthOfMonth()

        // Count actual weekdays standard Mon-Fri
        var workDaysCount = 0
        for (day in 1..daysInMonth) {
            val dow = ym.atDay(day).dayOfWeek.value
            if (dow in 1..5) {
                workDaysCount++
            }
        }

        val dailyRate = if (workDaysCount > 0) scheme.base / workDaysCount else 0.0

        var workedCount = 0
        var sickCount = 0
        var absentCount = 0
        var bonusSum = 0.0

        for (day in 1..daysInMonth) {
            val dayKey = day.toString()
            val dow = ym.atDay(day).dayOfWeek.value
            val typeId = dayData[dayKey]
            val type = scheme.dayTypes.find { it.id == typeId }

            if (dow in 1..5) {
                // Regular weekday
                if (type != null) {
                    if (type.deduct) {
                        absentCount++
                    } else if (type.sick) {
                        sickCount++
                    } else {
                        workedCount++
                    }
                    if (type.bonus > 0) {
                        bonusSum += type.bonus
                    }
                } else {
                    // Default assume worked as scheduled
                    workedCount++
                }
            } else if (dow == 6) {
                // Saturday
                if (type != null && type.weekend && type.id == "saturday") {
                    bonusSum += type.bonus
                }
            }
        }

        val basePay = dailyRate * workedCount + dailyRate * sickCount * (scheme.sickRate / 100.0)
        val premiumAdded = if (absentCount == 0 && sickCount == 0) scheme.premium else 0.0
        val deductions = dailyRate * absentCount + dailyRate * sickCount * (1.0 - scheme.sickRate / 100.0)
        val total = basePay + bonusSum + premiumAdded

        return MonthSummary(
            basePay = basePay,
            premium = premiumAdded,
            bonus = bonusSum,
            deductions = deductions,
            worked = workedCount,
            workDays = workDaysCount,
            sick = sickCount,
            total = total,
            dailyRate = dailyRate
        )
    }

    fun getDefaultScheme(): SalaryScheme {
        return repository.getDefaultScheme()
    }
}

data class MonthSummary(
    val basePay: Double,
    val premium: Double,
    val bonus: Double,
    val deductions: Double,
    val worked: Int,
    val workDays: Int,
    val sick: Int,
    val total: Double,
    val dailyRate: Double
) {
    companion object {
        val EMPTY = MonthSummary(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0, 0.0)
    }
}
