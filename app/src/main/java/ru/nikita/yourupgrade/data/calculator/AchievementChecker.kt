package ru.nikita.yourupgrade.data.calculator

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.local.entity.AppUsageEntity
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.model.AchievementType
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AchievementChecker {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val mutex = Mutex()  // Mutex для синхронизации — только одна проверка одновременно
    private val FULL_DAYS_THRESHOLDS = listOf(3, 10, 30)
    private val FULL_DAYS_STREAK_THRESHOLDS = listOf(3, 7, 15)
    private val HABIT_STREAK_THRESHOLDS = listOf(5, 15, 21)
    private val APP_USAGE_THRESHOLDS = listOf(7, 21, 60)
    private val LEVEL_POINTS = listOf(2, 3, 4)
    private const val MIN_HABITS_FOR_ACHIEVEMENTS = 4

    fun checkAppUsage(context: Context, onNewAchievements: (List<AchievementEntity>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val db = AppDatabase.getDatabase(context)
                val todayStr = LocalDate.now().format(dateFormatter)

                // Записываем заход
                val existingUsage = db.appUsageDao().getUsageByDate(todayStr)
                if (existingUsage == null) {
                    db.appUsageDao().insertUsage(AppUsageEntity(date = todayStr))
                }

                val existingAchievements = db.achievementDao().getAllAchievementsSync()
                val newAchievements = mutableListOf<AchievementEntity>()

                checkAppUsageAchievements(db, existingAchievements, todayStr, newAchievements)

                newAchievements.forEach { db.achievementDao().insertAchievement(it) }

                withContext(Dispatchers.Main) {
                    onNewAchievements(newAchievements)
                }
            }
        }
    }

    fun checkHabitRelated(context: Context, onNewAchievements: (List<AchievementEntity>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val db = AppDatabase.getDatabase(context)
                val today = LocalDate.now()

                val allHabits = db.habitDao().getAllHabitsSync()
                val dailyHabits = allHabits.filter { it.type == HabitType.DAILY && it.isActive }

                // Минимум 4 привычки
                if (dailyHabits.size < MIN_HABITS_FOR_ACHIEVEMENTS) {
                    withContext(Dispatchers.Main) {
                        onNewAchievements(emptyList())
                    }
                    return@launch
                }

                val allLogs = db.habitLogDao().getAllLogsSync()
                val existingAchievements = db.achievementDao().getAllAchievementsSync()

                val newAchievements = mutableListOf<AchievementEntity>()

                checkFullDaysAchievements(
                    dailyHabits,
                    allLogs,
                    existingAchievements,
                    today,
                    newAchievements
                )
                checkFullDaysStreakAchievements(
                    dailyHabits,
                    allLogs,
                    existingAchievements,
                    today,
                    newAchievements
                )
                checkHabitStreakAchievements(
                    dailyHabits,
                    allLogs,
                    existingAchievements,
                    today,
                    newAchievements
                )

                newAchievements.forEach { db.achievementDao().insertAchievement(it) }

                withContext(Dispatchers.Main) {
                    onNewAchievements(newAchievements)
                }
            }
        }
    }

    // === 1. Полные дни ===
    private fun checkFullDaysAchievements(
        dailyHabits: List<HabitEntity>,
        allLogs: List<HabitLogEntry>,
        existingAchievements: List<AchievementEntity>,
        today: LocalDate,
        newAchievements: MutableList<AchievementEntity>
    ) {
        val logsByDate = allLogs.groupBy { it.date }

        var fullDaysCount = 0
        val minCreatedDate = dailyHabits.minOf { habit ->
            Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        var checkDate = minCreatedDate
        while (!checkDate.isAfter(today)) {
            if (isFullDay(dailyHabits, logsByDate, checkDate)) {
                fullDaysCount++
            }
            checkDate = checkDate.plusDays(1)
        }

        FULL_DAYS_THRESHOLDS.forEachIndexed { index, threshold ->
            val level = index + 1
            if (fullDaysCount >= threshold && !isEarnedGlobal(
                    existingAchievements,
                    newAchievements,
                    AchievementType.FULL_DAYS,
                    level
                )
            ) {
                newAchievements.add(
                    AchievementEntity(
                        type = AchievementType.FULL_DAYS,
                        level = level,
                        earnedDate = today.format(dateFormatter),
                        points = LEVEL_POINTS[index]
                    )
                )
            }
        }
    }

    // === 2. Серия полных дней ===
    private fun checkFullDaysStreakAchievements(
        dailyHabits: List<HabitEntity>,
        allLogs: List<HabitLogEntry>,
        existingAchievements: List<AchievementEntity>,
        today: LocalDate,
        newAchievements: MutableList<AchievementEntity>
    ) {
        val logsByDate = allLogs.groupBy { it.date }

        var streak = 0
        var checkDate = today
        val minCreatedDate = dailyHabits.minOf { habit ->
            Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        while (!checkDate.isBefore(minCreatedDate)) {
            if (isFullDay(dailyHabits, logsByDate, checkDate)) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        FULL_DAYS_STREAK_THRESHOLDS.forEachIndexed { index, threshold ->
            val level = index + 1
            if (streak >= threshold && !isEarnedGlobal(
                    existingAchievements,
                    newAchievements,
                    AchievementType.FULL_DAYS_STREAK,
                    level
                )
            ) {
                newAchievements.add(
                    AchievementEntity(
                        type = AchievementType.FULL_DAYS_STREAK,
                        level = level,
                        earnedDate = today.format(dateFormatter),
                        points = LEVEL_POINTS[index]
                    )
                )
            }
        }
    }

    // === 3. Привычка подряд (глобальная ачивка) ===
    private fun checkHabitStreakAchievements(
        dailyHabits: List<HabitEntity>,
        allLogs: List<HabitLogEntry>,
        existingAchievements: List<AchievementEntity>,
        today: LocalDate,
        newAchievements: MutableList<AchievementEntity>
    ) {
        val logsByDate = allLogs.groupBy { it.date }

        // 🆕 Находим максимальный стрик среди всех привычек
        var maxStreak = 0
        var firstHabitWithMaxStreak: Long? = null

        for (habit in dailyHabits) {
            val streak = calculateHabitStreak(habit, logsByDate, today)
            if (streak > maxStreak) {
                maxStreak = streak
                firstHabitWithMaxStreak = habit.id
            }
        }

        // 🆕 Начисляем ачивки глобально (без привязки к привычке)
        HABIT_STREAK_THRESHOLDS.forEachIndexed { index, threshold ->
            val level = index + 1
            if (maxStreak >= threshold && !isEarnedGlobal(
                    existingAchievements,
                    newAchievements,
                    AchievementType.HABIT_STREAK,
                    level
                )
            ) {
                newAchievements.add(
                    AchievementEntity(
                        type = AchievementType.HABIT_STREAK,
                        level = level,
                        habitId = firstHabitWithMaxStreak,  // Запоминаем какая привычка первая достигла
                        earnedDate = today.format(dateFormatter),
                        points = LEVEL_POINTS[index]
                    )
                )
            }
        }
    }

    // 🆕 Вспомогательный метод: подсчёт стрика для одной привычки
    private fun calculateHabitStreak(
        habit: HabitEntity,
        logsByDate: Map<String, List<HabitLogEntry>>,
        today: LocalDate
    ): Int {
        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
       // 🆕 Проверяем, есть ли отметка за сегодня
        val todayStr = today.format(dateFormatter)
        val todayLog = logsByDate[todayStr]?.find { it.habitId == habit.id }
        val todayDone = todayLog != null && todayLog.earnedPoints >= 0 &&
                (habit.completionMode != CompletionMode.MEASURABLE ||
                        todayLog.completedValue / habit.targetValue >= 0.5f)


        var streak = 0
        var checkDate = if (todayDone) today else today.minusDays(1)

        while (!checkDate.isBefore(createdDate)) {
            val dateStr = checkDate.format(dateFormatter)
            val dayLogs = logsByDate[dateStr] ?: emptyList()
            val habitLog = dayLogs.find { it.habitId == habit.id }

            val isDone = when {
                habitLog == null -> false
                habitLog.earnedPoints < 0 -> false
                habit.completionMode == CompletionMode.MEASURABLE -> {
                    val percent = if (habit.targetValue > 0) {
                        habitLog.completedValue / habit.targetValue
                    } else 0f
                    percent >= 0.5f
                }

                else -> true
            }

            if (isDone) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    // 🆕 Проверка без привязки к привычке (глобальная ачивка)
    private fun isEarnedGlobal(
        existing: List<AchievementEntity>,
        new: List<AchievementEntity>,
        type: AchievementType,
        level: Int
    ): Boolean {
        return existing.any { it.type == type && it.level == level } ||
                new.any { it.type == type && it.level == level }
    }

    // === 4. Заход в приложение ===
    private suspend fun checkAppUsageAchievements(
        db: AppDatabase,
        existingAchievements: List<AchievementEntity>,
        todayStr: String,
        newAchievements: MutableList<AchievementEntity>
    ) {
        val totalUsageDays = db.appUsageDao().getTotalUsageDays()

        APP_USAGE_THRESHOLDS.forEachIndexed { index, threshold ->
            val level = index + 1
            if (totalUsageDays >= threshold && !isEarnedGlobal(
                    existingAchievements,
                    newAchievements,
                    AchievementType.APP_USAGE,
                    level
                )
            ) {
                newAchievements.add(
                    AchievementEntity(
                        type = AchievementType.APP_USAGE,
                        level = level,
                        earnedDate = todayStr,
                        points = LEVEL_POINTS[index]
                    )
                )
            }
        }
    }

    private fun isFullDay(
        dailyHabits: List<HabitEntity>,
        logsByDate: Map<String, List<HabitLogEntry>>,
        date: LocalDate
    ): Boolean {
        val dateStr = date.format(dateFormatter)
        val dayLogs = logsByDate[dateStr] ?: emptyList()

        val activeHabits = dailyHabits.filter { habit ->
            val createdDate = Instant.ofEpochMilli(habit.createdAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            !createdDate.isAfter(date)
        }

        if (activeHabits.isEmpty()) return false

        for (habit in activeHabits) {
            val habitLog = dayLogs.find { it.habitId == habit.id }

            val isDone = when {
                habitLog == null -> false
                habitLog.earnedPoints < 0 -> false
                habit.completionMode == CompletionMode.MEASURABLE -> {
                    val percent = if (habit.targetValue > 0) {
                        habitLog.completedValue / habit.targetValue
                    } else 0f
                    percent >= 0.5f
                }

                else -> true
            }

            if (!isDone) return false
        }

        return true
    }
}