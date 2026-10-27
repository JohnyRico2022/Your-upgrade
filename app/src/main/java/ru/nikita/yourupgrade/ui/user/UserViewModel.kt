package ru.nikita.yourupgrade.ui.user

import android.app.Application
import androidx.compose.remote.creation.toFloat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.data.calculator.PointsCalculator
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.data.repository.HabitRepository
import ru.nikita.yourupgrade.util.PreferencesManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val prefs = PreferencesManager(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val uiState: StateFlow<UserUiState>

    init {
        val db = AppDatabase.getDatabase(application)

        repository = HabitRepository(
            db.habitDao(),
            db.categoryDao(),
            db.habitLogDao(),
            db.achievementDao()
        )

        uiState = combine(
            repository.getAllCategories(),
            repository.getAllLogs(),
            repository.getActiveHabits(),
            ::buildUiState
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserUiState(userName = prefs.userName)
        )
    }

    private suspend fun buildUiState(
        categories: List<SkillCategoryEntity>,
        logs: List<HabitLogEntry>,
        habits: List<HabitEntity>
    ): UserUiState {
        // 1. Считаем баллы по каждой категории
        val categoryMap = categories.associateBy { it.id }
        val habitsById = habits.associateBy { it.id }

        val pointsByCategory = mutableMapOf<Long, Float>()
        for (cat in categories) {
            pointsByCategory[cat.id] = 0f
        }

        // 🆕 Разделяем логи по типу привычки
        val dailyHabits = habits.filter { it.type == HabitType.DAILY }
        val weeklyHabits = habits.filter { it.type == HabitType.WEEKLY }

        val dailyHabitIds = dailyHabits.map { it.id }.toSet()
        val weeklyHabitIds = weeklyHabits.map { it.id }.toSet()

        // 1. Ежедневные: просто суммируем earnedPoints
        for (log in logs) {
            if (log.habitId !in dailyHabitIds) continue
            val habit = habitsById[log.habitId] ?: continue
            val current = pointsByCategory[habit.categoryId] ?: 0f
            pointsByCategory[habit.categoryId] = current + log.earnedPoints
        }

        // 2. 🆕 Еженедельные: группируем по неделям и пересчитываем
        val weeklyLogsByHabit = logs.filter { it.habitId in weeklyHabitIds }
            .groupBy { it.habitId }

        for (habit in weeklyHabits) {
            val habitLogs = weeklyLogsByHabit[habit.id] ?: emptyList()

            // Группируем логи этой привычки по неделям
            val logsByWeek = habitLogs.groupBy { log ->
                LocalDate.parse(log.date, dateFormatter)
                    .with(DayOfWeek.MONDAY)
            }

            // Считаем баллы за каждую неделю
            var totalWeeklyPoints = 0f
            for ((_, weekLogs) in logsByWeek) {
                val weekTotal = weekLogs.sumOf { it.completedValue.toDouble() }.toFloat()

                val weekPoints = PointsCalculator.calculatePoints(
                    completionMode = habit.completionMode,
                    completedValue = weekTotal,
                    targetValue = habit.targetValue,
                    habitType = HabitType.WEEKLY
                )

                totalWeeklyPoints += weekPoints
            }

            // Добавляем к категории
            val current = pointsByCategory[habit.categoryId] ?: 0f
            pointsByCategory[habit.categoryId] = current + totalWeeklyPoints
        }


        // 🆕 Получаем очки за ачивки
        val achievementPoints = repository.getTotalAchievementPoints()?.toFloat() ?: 0f

        // Общий прогресс включает очки за ачивки
        val totalPoints = (pointsByCategory.values.sum() + achievementPoints).coerceAtLeast(0f)
        val level = PointsCalculator.getLevel(totalPoints)

        // 🆕 ПРЯМОЙ ПОДСЧЁТ: показываем реальные баллы / порог следующего уровня
        val nextLevelThreshold = PointsCalculator.getLevelMaxPoints(level)
        val progressLabel = String.format(
            "%.2f / %.0f",
            totalPoints,
            nextLevelThreshold
        )

        // Прогресс-бар: от 0 до порога следующего уровня
        val progress = if (nextLevelThreshold > 0) {
            (totalPoints / nextLevelThreshold).coerceIn(0f, 1f)
        } else 1f


        /** Прогресс по категориям */
        val categoryProgress = categories.map { cat ->

            val points = (pointsByCategory[cat.id] ?: 0f).coerceAtLeast(0f)

            val habitsOfCategory = habits.filter { it.categoryId == cat.id }
            val habitPointsList = habitsOfCategory.map { habit ->
                val habitLogs = logs.filter { it.habitId == habit.id }

                // 🆕 Для еженедельных пересчитываем по неделям
                val habitTotalPoints = if (habit.type == HabitType.WEEKLY) {
                    val logsByWeek = habitLogs.groupBy { log ->
                        LocalDate.parse(log.date, dateFormatter)
                            .with(DayOfWeek.MONDAY)
                    }

                    logsByWeek.values.sumOf { weekLogs ->
                        val weekTotal = weekLogs.sumOf { it.completedValue.toDouble() }.toFloat()
                        PointsCalculator.calculatePoints(
                            completionMode = habit.completionMode,
                            completedValue = weekTotal,
                            targetValue = habit.targetValue,
                            habitType = HabitType.WEEKLY
                        ).toDouble()
                    }.toFloat()
                } else
                    habitLogs.sumOf { it.earnedPoints.toDouble() }
                        .toFloat()  // Для ежедневных просто сумма

                HabitPointsUi(
                    habitId = habit.id,
                    habitName = habit.name,
                    points = habitTotalPoints
                )
            }.sortedByDescending { it.points }

            CategoryProgress(
                id = cat.id,
                name = cat.name,
                emoji = cat.iconEmoji,
                points = points,
                level = PointsCalculator.getLevel(points),
                habits = habitPointsList
            )
        }.toMutableList()

        // 🆕 Привычки с удалёнными категориями → карточка "Без категории"
        val existingCategoryIds = categories.map { it.id }.toSet()
        val habitsWithoutCategory = habits.filter { it.categoryId !in existingCategoryIds }

        if (habitsWithoutCategory.isNotEmpty()) {
            val habitPointsList = habitsWithoutCategory.map { habit ->
                val habitLogs = logs.filter { it.habitId == habit.id }
                val habitTotalPoints = habitLogs.sumOf { it.earnedPoints.toDouble() }
                    .toFloat()
                    .coerceAtLeast(0f)

                HabitPointsUi(
                    habitId = habit.id,
                    habitName = habit.name,
                    points = habitTotalPoints
                )
            }.sortedByDescending { it.points }

            val noCategoryPoints = habitsWithoutCategory.sumOf { habit ->
                logs.filter { it.habitId == habit.id }
                    .sumOf { it.earnedPoints.toDouble() }
            }.toFloat().coerceAtLeast(0f)

            categoryProgress.add(
                CategoryProgress(
                    id = -1L,
                    name = getApplication<Application>().getString(R.string.user_no_category),
                    emoji = "📌",
                    points = noCategoryPoints,
                    level = PointsCalculator.getLevel(noCategoryPoints),
                    habits = habitPointsList
                )
            )
        }

        /** Статистика за сегодня */
        val today = LocalDate.now().format(dateFormatter)
        val todayLogs = logs.filter { it.date == today }

        val todayDailyLogs = todayLogs.count { log ->
            habitsById[log.habitId]?.type == HabitType.DAILY
        }
        val todayWeeklyLogs = todayLogs.count { log ->
            habitsById[log.habitId]?.type == HabitType.WEEKLY
        }

        val dailyTotal = habits.count { it.type == HabitType.DAILY }
        val weeklyTotal = habits.count { it.type == HabitType.WEEKLY }

        return UserUiState(
            userName = prefs.userName,
            totalPoints = totalPoints,
            currentLevel = level,
            levelMinPoints = PointsCalculator.getLevelMinPoints(level),
            levelMaxPoints = nextLevelThreshold,
            progressToNextLevel = progress,
            progressLabel = progressLabel,
            motivationQuote = PointsCalculator.getMotivationQuote(level),
            categories = categoryProgress,
            todayDoneCount = todayDailyLogs,
            todayTotalCount = dailyTotal,
            weekDoneCount = todayWeeklyLogs,
            weekTotalCount = weeklyTotal
        )
    }
}