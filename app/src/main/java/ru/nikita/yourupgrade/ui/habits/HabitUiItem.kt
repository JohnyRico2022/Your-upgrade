package ru.nikita.yourupgrade.ui.habits

import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType

data class HabitUiItem(
    val id: Long,
    val name: String,
    val categoryName: String,
    val categoryEmoji: String,
    val type: HabitType,
    val completionMode: CompletionMode,
    val targetValue: Float,
    val isDoneToday: Boolean = false,
    val completedValue: Float = 0f,
    val unit: String
) {
    val targetLabel: String
        get() = when (completionMode) {
            CompletionMode.MEASURABLE -> "Цель: ${targetValue.toLong()}"
            CompletionMode.CHECKABLE -> "Отмечаемое"
        }

    val progressLabel: String
        get() = "${completedValue.toLong()}/${targetValue.toLong()}"

    val isFullyDone: Boolean
        get() = completedValue >= targetValue
}