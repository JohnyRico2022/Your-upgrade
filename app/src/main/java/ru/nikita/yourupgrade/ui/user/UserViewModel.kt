package ru.nikita.yourupgrade.ui.user


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import ru.nikita.yourupgrade.data.calculator.PointsCalculator
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.repository.HabitRepository
import ru.nikita.yourupgrade.util.PreferencesManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val prefs = PreferencesManager(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val uiState: StateFlow<UserUiState>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao(), db.categoryDao(), db.habitLogDao())

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

    private fun buildUiState(
        categories: List<ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity>,
        logs: List<ru.nikita.yourupgrade.data.local.entity.HabitLogEntry>,
        habits: List<ru.nikita.yourupgrade.data.local.entity.HabitEntity>
    ): UserUiState {
        // 1. Считаем баллы по каждой категории
        val categoryMap = categories.associateBy { it.id }
        val habitsById = habits.associateBy { it.id }

        val pointsByCategory = mutableMapOf<Long, Float>()
        for (cat in categories) {
            pointsByCategory[cat.id] = 0f
        }

        for (log in logs) {
            val habit = habitsById[log.habitId] ?: continue
            val current = pointsByCategory[habit.categoryId] ?: 0f
            pointsByCategory[habit.categoryId] = current + log.earnedPoints
        }

        // 2. Общий прогресс
        val totalPoints = pointsByCategory.values.sum()
        val level = PointsCalculator.getLevel(totalPoints)
        val levelMin = PointsCalculator.getLevelMinPoints(level)
        val levelMax = PointsCalculator.getLevelMaxPoints(level)

        val progress = if (levelMax > levelMin) {
            ((totalPoints - levelMin) / (levelMax - levelMin)).coerceIn(0f, 1f)
        } else 1f

        val progressLabel = String.format(
            "%.2f / %.0f",
            totalPoints - levelMin,
            levelMax - levelMin
        )

        // 3. Прогресс по категориям
        val categoryProgress = categories.map { cat ->
            val points = pointsByCategory[cat.id] ?: 0f
            CategoryProgress(
                id = cat.id,
                name = cat.name,
                emoji = cat.iconEmoji,
                points = points,
                level = PointsCalculator.getLevel(points)
            )
        }

        // 4. Статистика за сегодня
        val today = LocalDate.now().format(dateFormatter)
        val todayLogs = logs.filter { it.date == today }
        val todayDoneCount = todayLogs.size
        val todayTotalCount = habits.count {
            it.type == ru.nikita.yourupgrade.data.model.HabitType.DAILY
        }

        return UserUiState(
            userName = prefs.userName,
            totalPoints = totalPoints,
            currentLevel = level,
            levelMinPoints = levelMin,
            levelMaxPoints = levelMax,
            progressToNextLevel = progress,
            progressLabel = progressLabel,
            motivationQuote = PointsCalculator.getMotivationQuote(level),
            categories = categoryProgress,
            todayDoneCount = todayDoneCount,
            todayTotalCount = todayTotalCount
        )
    }
}