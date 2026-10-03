package ru.nikita.yourupgrade.data.local.dao

import androidx.room.*
import ru.nikita.yourupgrade.data.local.entity.AppUsageEntity

@Dao
interface AppUsageDao {

    @Query("SELECT * FROM app_usage WHERE date = :date")
    suspend fun getUsageByDate(date: String): AppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AppUsageEntity)

    @Query("SELECT COUNT(*) FROM app_usage")
    suspend fun getTotalUsageDays(): Int
}