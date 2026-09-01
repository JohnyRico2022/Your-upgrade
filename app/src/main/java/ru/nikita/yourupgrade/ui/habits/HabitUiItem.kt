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
    val targetValue: Float
) {
    val targetLabel: String
        get() = when (completionMode) {
            CompletionMode.MEASURABLE -> {
                val value = if (targetValue == targetValue.toLong().toFloat()) {
                    targetValue.toLong().toString()
                } else {
                    targetValue.toString()
                }
                "Цель: $value"
            }
            CompletionMode.CHECKABLE -> "Отмечаемое"
        }
}