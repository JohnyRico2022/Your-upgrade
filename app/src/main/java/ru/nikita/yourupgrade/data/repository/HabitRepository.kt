package ru.nikita.yourupgrade.data.repository

import kotlinx.coroutines.flow.Flow
import ru.nikita.yourupgrade.data.local.dao.AchievementDao
import ru.nikita.yourupgrade.data.local.dao.CategoryDao
import ru.nikita.yourupgrade.data.local.dao.HabitDao
import ru.nikita.yourupgrade.data.local.dao.HabitLogDao
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.data.model.LogStatus

class HabitRepository(
    private val habitDao: HabitDao,
    private val categoryDao: CategoryDao,
    private val habitLogDao: HabitLogDao,
    private val achievementDao: AchievementDao
) {

    fun getDailyHabits(): Flow<List<HabitEntity>> =
        habitDao.getActiveHabitsByType(HabitType.DAILY)

    fun getWeeklyHabits(): Flow<List<HabitEntity>> =
        habitDao.getActiveHabitsByType(HabitType.WEEKLY)

    fun getAllCategories(): Flow<List<SkillCategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<SkillCategoryEntity> =
        categoryDao.getAllCategoriesSync()

    suspend fun insertHabit(habit: HabitEntity): Long =
        habitDao.insertHabit(habit)

    suspend fun deleteHabit(id: Long) =
        habitDao.deleteHabit(id)

    suspend fun getCategoryById(id: Long): SkillCategoryEntity? =
        categoryDao.getCategoryById(id)

    fun getLogsForDate(date: String): Flow<List<HabitLogEntry>> =
        habitLogDao.getLogsForDate(date)

    suspend fun getLogForHabitAndDate(habitId: Long, date: String): HabitLogEntry? =
        habitLogDao.getLogForHabitAndDate(habitId, date)

    suspend fun markHabitDone(habitId: Long, date: String, points: Float = 1f) {
        habitLogDao.insertLog(
            HabitLogEntry(
                habitId = habitId,
                date = date,
                completedValue = 1f,
                earnedPoints = points,
                status = LogStatus.DONE
            )
        )
    }

    fun getAllLogs(): Flow<List<HabitLogEntry>> =
        habitLogDao.getAllLogs()

    fun getActiveHabits(): Flow<List<HabitEntity>> =
        habitDao.getActiveHabits()

    suspend fun unmarkHabit(habitId: Long, date: String) {
        habitLogDao.deleteLogByHabitAndDate(habitId, date)
    }

    suspend fun updateLog(habitId: Long, date: String, value: Float, points: Float) {
        habitLogDao.updateLog(habitId, date, value, points)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun insertLog(entry: HabitLogEntry) {
        habitLogDao.insertLog(entry)
    }

    fun getLogsForHabitInRange(
        habitId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<HabitLogEntry>> =
        habitLogDao.getLogsForHabitInRange(habitId, startDate, endDate)

    suspend fun getHabitById(id: Long): HabitEntity? =
        habitDao.getHabitById(id)

    suspend fun getTotalAchievementPoints(): Int? {
        return achievementDao.getTotalAchievementPoints()
    }
}