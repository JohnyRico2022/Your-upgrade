package ru.nikita.yourupgrade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.nikita.yourupgrade.data.model.LogStatus

@Entity(tableName = "habit_logs")
data class HabitLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String,  // "yyyy-MM-dd"
    val completedValue: Float = 1f,
    val earnedPoints: Float = 1f,
    val status: LogStatus = LogStatus.DONE,
    val timestamp: Long = System.currentTimeMillis()
)

