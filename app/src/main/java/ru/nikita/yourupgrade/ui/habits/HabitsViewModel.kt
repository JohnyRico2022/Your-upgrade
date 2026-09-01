package ru.nikita.yourupgrade.ui.habits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.repository.HabitRepository
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType

class HabitsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    val dailyHabits: Flow<List<HabitEntity>>
    val weeklyHabits: Flow<List<HabitEntity>>
    val categories: Flow<List<SkillCategoryEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao(), db.categoryDao())

        dailyHabits = repository.getDailyHabits()
        weeklyHabits = repository.getWeeklyHabits()
        categories = repository.getAllCategories()
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