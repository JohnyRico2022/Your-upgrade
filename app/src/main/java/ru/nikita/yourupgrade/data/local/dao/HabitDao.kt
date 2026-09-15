package ru.nikita.yourupgrade.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.model.HabitType

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isActive = 1 AND type = :type ORDER BY name COLLATE NOCASE ASC")
    fun getActiveHabitsByType(type: HabitType): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isActive = 0 WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitByIdFlow(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsSync(): List<HabitEntity>
}