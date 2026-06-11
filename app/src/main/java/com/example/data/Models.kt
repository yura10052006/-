package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DayType(
    val id: String,
    val name: String,
    val bonus: Double,
    val color: String,
    val deduct: Boolean = false,
    val sick: Boolean = false,
    val weekend: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SalaryScheme(
    val base: Double,
    val premium: Double,
    val sickRate: Double, // percentage e.g. 60.0
    val dayTypes: List<DayType>,
    val defaultWeekday: Map<String, String> // e.g. "1" -> "gym", "2" -> "full-bonus", etc. 1-Mon..7-Sun
)

@JsonClass(generateAdapter = true)
data class SavedMonth(
    val dayData: Map<String, String>, // day of month as string e.g. "1" -> "gym"
    val total: Double,
    val worked: Int,
    val workDays: Int,
    val sick: Int,
    val bonus: Double
)

@JsonClass(generateAdapter = true)
data class UserModel(
    val password: String,
    val role: String, // "admin" or "worker"
    val displayName: String,
    val scheme: SalaryScheme,
    val months: Map<String, SavedMonth> // key "YYYY-MM"
)

@JsonClass(generateAdapter = true)
data class SalaryDatabase(
    val users: Map<String, UserModel>
)
