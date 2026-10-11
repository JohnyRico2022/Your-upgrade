package ru.nikita.yourupgrade.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM habit_logs")
    fun getAllLogs(): Flow<List<HabitLogEntry>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogsSync(): List<HabitLogEntry>

    @Query("UPDATE habit_logs SET completedValue = :value, earnedPoints = :points, targetValue = :targetValue WHERE habitId = :habitId AND date = :date")
    suspend fun updateLog(habitId: Long, date: String, value: Float, points: Float, targetValue: Float)

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getLogsForDateRange(startDate: String, endDate: String): List<HabitLogEntry>
}