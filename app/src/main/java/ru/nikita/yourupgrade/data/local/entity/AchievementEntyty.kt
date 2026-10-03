package ru.nikita.yourupgrade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.nikita.yourupgrade.data.model.AchievementType


@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: AchievementType,
    val level: Int,            // 1, 2, 3
    val habitId: Long? = null, // Для HABIT_STREAK
    val earnedDate: String,
    val points: Int            // 2, 3, 4
)