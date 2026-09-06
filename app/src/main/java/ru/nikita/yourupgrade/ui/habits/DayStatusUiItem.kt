package ru.nikita.yourupgrade.ui.habits

import ru.nikita.yourupgrade.data.model.CompletionMode

data class DayStatusUiItem(
    val dayShortName: String,   // "ПН", "ВТ", ...
    val date: String,           // "2024-01-15"
    val isDone: Boolean,
    val completedValue: Float,
    val targetValue: Float,
    val isToday: Boolean,
    val completionMode: CompletionMode
) {
    val statusLabel: String
        get() = when (completionMode) {
            CompletionMode.CHECKABLE -> if (isDone) "✓" else "–"
            CompletionMode.MEASURABLE -> {
                if (completedValue > 0f) {
                    "${completedValue.toLong()}/${targetValue.toLong()}"
                } else {
                    "–"
                }
            }
        }

    val isFullyDone: Boolean
        get() = when (completionMode) {
            CompletionMode.CHECKABLE -> isDone
            CompletionMode.MEASURABLE -> completedValue >= targetValue
        }
}