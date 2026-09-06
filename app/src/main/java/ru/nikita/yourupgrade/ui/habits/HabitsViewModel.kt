package ru.nikita.yourupgrade.ui.habits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.repository.HabitRepository
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val dailyHabits: Flow<List<HabitEntity>>
    val weeklyHabits: Flow<List<HabitEntity>>
    val categories: Flow<List<SkillCategoryEntity>>

    val logsForDate: Flow<List<HabitLogEntry>> = _selectedDate.flatMapLatest { date ->
        repository.getLogsForDate(date.format(dateFormatter))
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao(), db.categoryDao(), db.habitLogDao())

        dailyHabits = repository.getDailyHabits()
        weeklyHabits = repository.getWeeklyHabits()
        categories = repository.getAllCategories()
    }

    // 🆕 Для CHECKABLE: переключение галочки
    fun toggleHabitCompletion(habitId: Long) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(dateFormatter)
            val existingLog = repository.getLogForHabitAndDate(habitId, dateStr)

            if (existingLog != null) {
                repository.unmarkHabit(habitId, dateStr)
            } else {
                repository.markHabitDone(habitId, dateStr)
            }
        }
    }

    // 🆕 Для MEASURABLE: сохранение значения
    fun saveMeasurableValue(habitId: Long, value: Float) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(dateFormatter)

            if (value <= 0f) {
                // Если ввели 0 — удаляем запись
                repository.unmarkHabit(habitId, dateStr)
            } else {
                // Иначе создаём/обновляем запись
                val log = HabitLogEntry(
                    habitId = habitId,
                    date = dateStr,
                    completedValue = value,
                    earnedPoints = 1f,  // пока фиксированно, формулы в v0.3
                    status = ru.nikita.yourupgrade.data.model.LogStatus.DONE
                )
                repository.insertLog(log)
            }
        }
    }


    fun addHabit(
        name: String,
        categoryId: Long,
        type: HabitType,
        completionMode: CompletionMode,
        targetValue: Float
    ) {
        viewModelScope.launch {
            val habit = HabitEntity(
                name = name,
                categoryId = categoryId,
                type = type,
                completionMode = completionMode,
                targetValue = if (completionMode == CompletionMode.CHECKABLE) 1f else targetValue
            )
            repository.insertHabit(habit)
        }
    }


    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }
}