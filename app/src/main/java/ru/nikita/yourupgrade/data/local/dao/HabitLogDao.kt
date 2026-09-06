package ru.nikita.yourupgrade.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<HabitLogEntry>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getLogForHabitAndDate(habitId: Long, date: String): HabitLogEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: HabitLogEntry)

    @Delete
    suspend fun deleteLog(entry: HabitLogEntry)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLogByHabitAndDate(habitId: Long, date: String)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getLogsForHabitInRange(habitId: Long, startDate: String, endDate: String): Flow<List<HabitLogEntry>>
}