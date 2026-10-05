package ru.nikita.yourupgrade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val type: HabitType = HabitType.DAILY,
    val completionMode: CompletionMode = CompletionMode.CHECKABLE,
    val targetValue: Float = 1f,
    val weeklyTarget: Int = 1,
    val unit: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)