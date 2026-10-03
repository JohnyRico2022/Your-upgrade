package ru.nikita.yourupgrade.ui.stats

import java.time.LocalDate

enum class DayHabitStatus {
    DONE,        // ✔️
    MISSED,      // ❌
    NOT_CREATED  // ➖ (привычка ещё не была создана)
}

data class HabitDayStatus(
    val habitId: Long,
    val habitName: String,
    val categoryEmoji: String,
    val status: DayHabitStatus,
    val earnedPoints: Float
)

data class DayStats(
    val date: LocalDate,
    val dayShortName: String,
    val points: Float,
    val habits: List<HabitDayStatus>
)

data class WeekStatsUiState(
    val weekNumber: Int = 0,
    val weekStart: LocalDate = LocalDate.now(),
    val weekEnd: LocalDate = LocalDate.now(),
    val days: List<DayStats> = emptyList(),
    val totalPoints: Float = 0f,
    val selectedDayIndex: Int = -1,
    val dailyHabitsPoints: Float = 0f,
    val weeklyHabitsPoints: Float = 0f,
    val achievementPoints: Int = 0
) {
    val selectedDay: DayStats?
        get() = if (selectedDayIndex in days.indices) days[selectedDayIndex] else null

    val dateRangeLabel: String
        get() {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
            return "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}"
        }
}