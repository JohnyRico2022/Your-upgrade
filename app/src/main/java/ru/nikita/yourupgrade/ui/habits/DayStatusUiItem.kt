package ru.nikita.yourupgrade.ui.habits

import ru.nikita.yourupgrade.data.model.CompletionMode

data class DayStatusUiItem(
    val dayShortName: String,
    val date: String,
    val status: DayCompletionStatus,
    val completedValue: Float,
    val targetValue: Float,
    val isToday: Boolean,
    val completionMode: CompletionMode
) {

    // Для совместимости можно оставить свойство
    val isDone: Boolean get() = status == DayCompletionStatus.DONE
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

enum class DayCompletionStatus {
    DONE,
    PARTIAL,
    MISSED,
    NOT_CREATED
}