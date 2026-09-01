package ru.nikita.yourupgrade.data.repository

import kotlinx.coroutines.flow.Flow
import ru.nikita.yourupgrade.data.local.dao.CategoryDao
import ru.nikita.yourupgrade.data.local.dao.HabitDao
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.HabitType

class HabitRepository(
    private val habitDao: HabitDao,
    private val categoryDao: CategoryDao
) {

    fun getDailyHabits(): Flow<List<HabitEntity>> =
        habitDao.getActiveHabitsByType(HabitType.DAILY)

    fun getWeeklyHabits(): Flow<List<HabitEntity>> =
        habitDao.getActiveHabitsByType(HabitType.WEEKLY)

    fun getAllCategories(): Flow<List<SkillCategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun insertHabit(habit: HabitEntity): Long =
        habitDao.insertHabit(habit)

    suspend fun deleteHabit(id: Long) =
        habitDao.deleteHabit(id)

    suspend fun getCategoryById(id: Long): SkillCategoryEntity? =
        categoryDao.getCategoryById(id)
}