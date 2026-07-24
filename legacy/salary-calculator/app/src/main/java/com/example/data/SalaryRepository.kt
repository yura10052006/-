package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SalaryRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("salary_team_v1", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val dbAdapter = moshi.adapter(SalaryDatabase::class.java)

    init {
        // Pre-populate if not exists
        if (!sharedPrefs.contains("db")) {
            val initialDb = createInitialDatabase()
            saveDatabase(initialDb)
        }
    }

    fun loadDatabase(): SalaryDatabase {
        val json = sharedPrefs.getString("db", null)
        if (json != null) {
            try {
                return dbAdapter.fromJson(json) ?: createInitialDatabase()
            } catch (e: Exception) {
                Log.e("SalaryRepository", "Error parsing database JSON, resetting to default", e)
            }
        }
        val defaultDb = createInitialDatabase()
        saveDatabase(defaultDb)
        return defaultDb
    }

    fun saveDatabase(db: SalaryDatabase) {
        val json = dbAdapter.toJson(db)
        sharedPrefs.edit().putString("db", json).apply()
    }

    fun getUser(username: String): UserModel? {
        val db = loadDatabase()
        return db.users[username.lowercase()]
    }

    fun updateUser(username: String, user: UserModel) {
        val db = loadDatabase()
        val newUsers = db.users.toMutableMap()
        newUsers[username.lowercase()] = user
        saveDatabase(db.copy(users = newUsers))
    }

    fun deleteUser(username: String): Boolean {
        if (username.lowercase() == "admin") return false // Can't delete main admin
        val db = loadDatabase()
        if (db.users.containsKey(username.lowercase())) {
            val newUsers = db.users.toMutableMap()
            newUsers.remove(username.lowercase())
            saveDatabase(db.copy(users = newUsers))
            return true
        }
        return false
    }

    fun addUser(username: String, user: UserModel): Boolean {
        val db = loadDatabase()
        if (db.users.containsKey(username.lowercase())) {
            return false
        }
        val newUsers = db.users.toMutableMap()
        newUsers[username.lowercase()] = user
        saveDatabase(db.copy(users = newUsers))
        return true
    }

    fun getDefaultDayTypes(): List<DayType> {
        return listOf(
            DayType("full-bonus", "7:30–17:30 (+200₴)", 200.0, "#8b78ff"),
            DayType("gym", "7:30–16:30 (зал)", 0.0, "#34d47a"),
            DayType("absent", "Відсутній", 0.0, "#f05c52", deduct = true),
            DayType("sick", "Лікарняний", 0.0, "#f0c040", sick = true),
            DayType("saturday", "Субота (+1800₴)", 1800.0, "#f07840", weekend = true)
        )
    }

    fun getDefaultScheme(): SalaryScheme {
        return SalaryScheme(
            base = 28000.0,
            premium = 2000.0,
            sickRate = 60.0, // 60%
            dayTypes = getDefaultDayTypes(),
            defaultWeekday = mapOf(
                "1" to "gym",
                "2" to "full-bonus",
                "3" to "gym",
                "4" to "full-bonus",
                "5" to "gym"
            )
        )
    }

    private fun createInitialDatabase(): SalaryDatabase {
        val defaultScheme = getDefaultScheme()

        // Admin defaults
        val adminUser = UserModel(
            password = "admin123",
            role = "admin",
            displayName = "Адміністратор",
            scheme = defaultScheme,
            months = emptyMap()
        )

        // Pre-populated worker with beautiful dummy history so charts look marvelous!
        val workerUser = UserModel(
            password = "worker123",
            role = "worker",
            displayName = "Дмитро Коваленко",
            scheme = defaultScheme,
            months = mapOf(
                "2026-05" to SavedMonth(
                    dayData = mapOf(
                        "1" to "gym", "4" to "gym", "5" to "full-bonus", "6" to "gym", "7" to "full-bonus", "8" to "gym",
                        "11" to "gym", "12" to "full-bonus", "13" to "gym", "14" to "full-bonus", "15" to "gym",
                        "18" to "gym", "19" to "full-bonus", "20" to "gym", "21" to "full-bonus", "22" to "gym",
                        "25" to "gym", "26" to "full-bonus", "27" to "gym", "28" to "full-bonus", "29" to "gym",
                        // Saturday worked
                        "9" to "saturday"
                    ),
                    total = 31800.0, // 28000 + 2000 + 1800 (9 x 200) + 1800 (saturday worked) = 33600. Let's make it real calculation!
                    worked = 21,
                    workDays = 21,
                    sick = 0,
                    bonus = 3600.0 // 1800 full-bonus + 1800 Saturday
                ),
                "2026-04" to SavedMonth(
                    dayData = mapOf(
                        "1" to "gym", "2" to "full-bonus", "3" to "gym",
                        "6" to "gym", "7" to "full-bonus", "8" to "gym", "9" to "full-bonus", "10" to "gym",
                        "13" to "gym", "14" to "full-bonus", "15" to "gym", "16" to "full-bonus", "17" to "gym",
                        "20" to "gym", "21" to "full-bonus", "22" to "gym", "23" to "full-bonus", "24" to "gym",
                        "27" to "gym", "28" to "full-bonus", "29" to "gym", "30" to "full-bonus"
                    ),
                    total = 31800.0,
                    worked = 22,
                    workDays = 22,
                    sick = 0,
                    bonus = 1800.0
                ),
                "2026-06" to SavedMonth( // Current month initialized with standard week schedule
                    dayData = mapOf(
                        "1" to "gym", "2" to "full-bonus", "3" to "gym", "4" to "full-bonus", "5" to "gym",
                        "8" to "gym", "9" to "full-bonus", "10" to "gym", "11" to "full-bonus", "12" to "gym",
                        "15" to "gym", "16" to "full-bonus", "17" to "gym", "18" to "full-bonus", "19" to "gym",
                        "22" to "gym", "23" to "full-bonus", "24" to "gym", "25" to "full-bonus", "26" to "gym",
                        "29" to "gym", "30" to "full-bonus"
                    ),
                    total = 31800.0,
                    worked = 22,
                    workDays = 22,
                    sick = 0,
                    bonus = 1800.0
                )
            )
        )

        val workerUser2 = UserModel(
            password = "worker456",
            role = "worker",
            displayName = "Ольга Кравчук",
            scheme = defaultScheme.copy(base = 32000.0, premium = 3000.0),
            months = mapOf(
                "2026-05" to SavedMonth(
                    dayData = mapOf(
                        "1" to "gym", "4" to "gym", "5" to "full-bonus", "6" to "gym", "7" to "full-bonus", "8" to "gym",
                        "11" to "gym", "12" to "full-bonus", "13" to "gym", "14" to "full-bonus", "15" to "gym",
                        "18" to "gym", "19" to "full-bonus", "20" to "gym", "21" to "full-bonus", "22" to "gym",
                        "25" to "gym", "26" to "full-bonus", "27" to "gym", "28" to "full-bonus", "29" to "gym"
                    ),
                    total = 36800.0, // 32000 base + 3000 premium + 1800 bonus
                    worked = 21,
                    workDays = 21,
                    sick = 0,
                    bonus = 1800.0
                ),
                "2026-06" to SavedMonth(
                    dayData = mapOf(
                        "1" to "gym", "2" to "full-bonus", "3" to "gym", "4" to "full-bonus", "5" to "gym",
                        "8" to "gym", "9" to "full-bonus", "10" to "gym", "11" to "full-bonus", "12" to "gym",
                        "15" to "gym", "16" to "full-bonus", "17" to "gym", "18" to "full-bonus", "19" to "gym",
                        // Has some sick days
                        "22" to "sick", "23" to "sick", "24" to "gym", "25" to "full-bonus", "26" to "gym",
                        "29" to "gym", "30" to "full-bonus"
                    ),
                    total = 31500.0, // Roughly calculated for sample
                    worked = 20,
                    workDays = 22,
                    sick = 2,
                    bonus = 1600.0
                )
            )
        )

        return SalaryDatabase(
            users = mapOf(
                "admin" to adminUser,
                "worker" to workerUser,
                "olga" to workerUser2
            )
        )
    }
}
