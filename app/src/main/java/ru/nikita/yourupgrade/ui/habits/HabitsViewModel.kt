package ru.nikita.yourupgrade.ui.habits

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.calculator.AchievementChecker
import ru.nikita.yourupgrade.data.calculator.PointsCalculator
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.repository.HabitRepository
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.data.model.LogStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    var onAchievementsEarned: ((List<AchievementEntity>) -> Unit)? = null
    val dailyHabits: Flow<List<HabitEntity>>
    val weeklyHabits: Flow<List<HabitEntity>>
    val categories: Flow<List<SkillCategoryEntity>>

    val logsForDate: Flow<List<HabitLogEntry>> = _selectedDate.flatMapLatest { date ->
        repository.getLogsForDate(date.format(dateFormatter))
    }

    // 🆕 Логи за текущую неделю — для еженедельных
    val logsForWeek: Flow<List<HabitLogEntry>> = _selectedDate.flatMapLatest { date ->
        val monday = date.with(DayOfWeek.MONDAY)
        val sunday = monday.plusDays(6)
        repository.getLogsForDateRange(
            monday.format(dateFormatter),
            sunday.format(dateFormatter)
        )
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(
            db.habitDao(),
            db.categoryDao(),
            db.habitLogDao(),
            db.achievementDao()
        )

        dailyHabits = repository.getDailyHabits()
        weeklyHabits = repository.getWeeklyHabits()
        categories = repository.getAllCategories()
    }

    /** Для CHECKABLE: переключение галочки */
    fun toggleHabitCompletion(habitId: Long) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(dateFormatter)
            val existingLog = repository.getLogForHabitAndDate(habitId, dateStr)

            if (existingLog != null) {
                repository.unmarkHabit(habitId, dateStr)
            } else {
                val habit = repository.getHabitById(habitId) ?: return@launch
                val points = PointsCalculator.calculatePoints(
                    habit.completionMode,
                    completedValue = 1f,
                    targetValue = habit.targetValue,
                    habitType = habit.type
                )
                val targetValue: Float = habit.targetValue

                repository.markHabitDone(habitId, dateStr, points, targetValue)

                checkHabitAchievements()
            }
        }
    }

    /** Для MEASURABLE: сохранение значения */
    fun saveMeasurableValue(habitId: Long, value: Float) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(dateFormatter)

            if (value <= 0f) {
                repository.unmarkHabit(habitId, dateStr)
                return@launch
            }

            val habit = repository.getHabitById(habitId) ?: return@launch
            val points = PointsCalculator.calculatePoints(
                habit.completionMode,
                completedValue = value,
                targetValue = habit.targetValue,
                habitType = habit.type
            )
            val targetValue = habit.targetValue

            // Проверяем, есть ли уже запись за сегодня
            val existingLog = repository.getLogForHabitAndDate(habitId, dateStr)

            if (existingLog != null) {
                // Обновляем существующую запись
                repository.updateLog(habitId, dateStr, value, points, targetValue)
            } else {
                // Создаём новую
                val log = HabitLogEntry(
                    habitId = habitId,
                    date = dateStr,
                    completedValue = value,
                    earnedPoints = points,
                    status = LogStatus.DONE,
                    targetValue = targetValue
                )
                repository.insertLog(log)
            }

            checkHabitAchievements()
        }
    }

    // Вызов проверки ачивок после отметки
    private fun checkHabitAchievements() {
        AchievementChecker.checkHabitRelated(getApplication()) { newAchievements ->
            if (newAchievements.isNotEmpty()) {
                onAchievementsEarned?.invoke(newAchievements)
            }
        }
    }

    fun addHabit(
        name: String,
        categoryId: Long,
        type: HabitType,
        completionMode: CompletionMode,
        targetValue: Float,
        unit: String
    ) {
        viewModelScope.launch {
            val habit = HabitEntity(
                name = name,
                categoryId = categoryId,
                type = type,
                completionMode = completionMode,
                targetValue = if (completionMode == CompletionMode.CHECKABLE) 1f else targetValue,
                unit = unit
            )
            repository.insertHabit(habit)
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }

    suspend fun getCategories(): List<SkillCategoryEntity> {
        return repository.getAllCategoriesSync()
    }
}