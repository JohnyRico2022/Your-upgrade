package ru.nikita.yourupgrade.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.model.AchievementType

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsSync(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    // 🆕 Удаление ачивки
    @Delete
    suspend fun deleteAchievement(achievement: AchievementEntity)

    @Query("SELECT SUM(points) FROM achievements")
    suspend fun getTotalAchievementPoints(): Int?

    @Query("SELECT COALESCE(SUM(points), 0) FROM achievements WHERE earnedDate BETWEEN :startDate AND :endDate")
    suspend fun getPointsForDateRange(startDate: String, endDate: String): Int

    @Query("SELECT * FROM achievements WHERE type = :type AND level = :level")
    suspend fun getAchievementsByTypeAndLevel(type: String, level: Int): List<AchievementEntity>
}