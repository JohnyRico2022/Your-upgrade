package ru.nikita.yourupgrade.ui.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.model.AchievementType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private const val MIN_HABITS_FOR_ACHIEVEMENTS = 4
    }

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadRewards()
    }

    fun loadRewards() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                buildRewardsState()
            }
            _uiState.value = state
        }
    }

    private suspend fun buildRewardsState(): RewardsUiState {
        val allHabits = db.habitDao().getAllHabitsSync()
        val dailyHabits = allHabits.filter { it.isActive }

        // 🆕 Проверяем количество привычек
        if (dailyHabits.size < MIN_HABITS_FOR_ACHIEVEMENTS) {
            return RewardsUiState(
                categories = emptyList(),
                totalAchievementPoints = 0,
                hasEnoughHabits = false,
                habitsCount = dailyHabits.size
            )
        }

        val achievements = db.achievementDao().getAllAchievementsSync()
        val totalPoints = db.achievementDao().getTotalAchievementPoints() ?: 0
        val allLogs = db.habitLogDao().getAllLogsSync()
        val totalUsageDays = db.appUsageDao().getTotalUsageDays()

        val categories = mutableListOf<AchievementCategoryUi>()

        // 1. Полные дни
        categories.add(
            buildCategory(
                type = AchievementType.FULL_DAYS,
                title = "Полные дни",
                description = "Все привычки выполнены за день",
                iconEmoji = "🏆",
                thresholds = listOf(3, 10, 30),
                achievements = achievements,
                currentProgress = countFullDays(dailyHabits, allLogs)
            )
        )

        // 2. Серия полных дней
        categories.add(
            buildCategory(
                type = AchievementType.FULL_DAYS_STREAK,
                title = "Серия полных дней",
                description = "Полные дни подряд",
                iconEmoji = "🔥",
                thresholds = listOf(3, 7, 15),
                achievements = achievements,
                currentProgress = countFullDaysStreak(dailyHabits, allLogs)
            )
        )

        // 3. Привычка подряд
        categories.add(
            buildCategory(
                type = AchievementType.HABIT_STREAK,
                title = "Мастер привычки",
                description = "Любая привычка выполнена подряд",
                iconEmoji = "💪",
                thresholds = listOf(5, 15, 21),
                achievements = achievements,
                currentProgress = countMaxHabitStreak(dailyHabits, allLogs)
            )
        )

        // 4. Заход в приложение
        categories.add(
            buildCategory(
                type = AchievementType.APP_USAGE,
                title = "Постоянный пользователь",
                description = "Ежедневный заход в приложение",
                iconEmoji = "📱",
                thresholds = listOf(7, 21, 60),
                achievements = achievements,
                currentProgress = totalUsageDays
            )
        )


        return RewardsUiState(
            categories = categories,
            totalAchievementPoints = totalPoints,
            hasEnoughHabits = true,
            habitsCount = dailyHabits.size
        )
    }

    private fun buildCategory(
        type: AchievementType,
        title: String,
        description: String,
        iconEmoji: String,
        thresholds: List<Int>,
        achievements: List<ru.nikita.yourupgrade.data.local.entity.AchievementEntity>,
        currentProgress: Int
    ): AchievementCategoryUi {
        val levels = thresholds.mapIndexed { index, threshold ->
            val level = index + 1
            val earned = achievements.find { it.type == type && it.level == level }

            AchievementLevelUi(
                level = level,
                threshold = threshold,
                points = listOf(2, 3, 4)[index],
                isEarned = earned != null,
                earnedDate = earned?.earnedDate,
                currentProgress = currentProgress
            )
        }

        return AchievementCategoryUi(
            type = type,
            title = title,
            description = description,
            iconEmoji = iconEmoji,
            levels = levels
        )
    }

    // === Подсчёт прогресса ===

    private fun countFullDays(
        dailyHabits: List<ru.nikita.yourupgrade.data.local.entity.HabitEntity>,
        allLogs: List<ru.nikita.yourupgrade.data.local.entity.HabitLogEntry>
    ): Int {
        if (dailyHabits.isEmpty()) return 0

        val logsByDate = allLogs.groupBy { it.date }
        val today = LocalDate.now()
        val minCreatedDate = dailyHabits.minOf { habit ->
            Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        var count = 0
        var checkDate = minCreatedDate
        while (!checkDate.isAfter(today.minusDays(1))) {
            if (isFullDay(dailyHabits, logsByDate, checkDate)) {
                count++
            }
            checkDate = checkDate.plusDays(1)
        }
        return count
    }

    private fun countFullDaysStreak(
        dailyHabits: List<ru.nikita.yourupgrade.data.local.entity.HabitEntity>,
        allLogs: List<ru.nikita.yourupgrade.data.local.entity.HabitLogEntry>
    ): Int {
        if (dailyHabits.isEmpty()) return 0

        val logsByDate = allLogs.groupBy { it.date }
        val today = LocalDate.now()
        val minCreatedDate = dailyHabits.minOf { habit ->
            Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        var streak = 0
        var checkDate = today.minusDays(1)
        while (!checkDate.isBefore(minCreatedDate)) {
            if (isFullDay(dailyHabits, logsByDate, checkDate)) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun countMaxHabitStreak(
        dailyHabits: List<ru.nikita.yourupgrade.data.local.entity.HabitEntity>,
        allLogs: List<ru.nikita.yourupgrade.data.local.entity.HabitLogEntry>
    ): Int {
        if (dailyHabits.isEmpty()) return 0

        val logsByDate = allLogs.groupBy { it.date }
        val today = LocalDate.now()
        var maxStreak = 0

        for (habit in dailyHabits) {
            val createdDate = Instant.ofEpochMilli(habit.createdAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()

            var streak = 0
            var checkDate = today.minusDays(1)

            while (!checkDate.isBefore(createdDate)) {
                val dateStr = checkDate.format(dateFormatter)
                val dayLogs = logsByDate[dateStr] ?: emptyList()
                val habitLog = dayLogs.find { it.habitId == habit.id }

                val isDone = when {
                    habitLog == null -> false
                    habitLog.earnedPoints < 0 -> false
                    habit.completionMode == ru.nikita.yourupgrade.data.model.CompletionMode.MEASURABLE -> {
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

            maxStreak = maxOf(maxStreak, streak)
        }

        return maxStreak
    }

    private fun isFullDay(
        dailyHabits: List<ru.nikita.yourupgrade.data.local.entity.HabitEntity>,
        logsByDate: Map<String, List<ru.nikita.yourupgrade.data.local.entity.HabitLogEntry>>,
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
                habit.completionMode == ru.nikita.yourupgrade.data.model.CompletionMode.MEASURABLE -> {
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