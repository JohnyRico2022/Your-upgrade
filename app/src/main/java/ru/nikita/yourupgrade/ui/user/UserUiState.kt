package ru.nikita.yourupgrade.ui.user

data class UserUiState(
    val userName: String = "",
    val totalPoints: Float = 0f,
    val currentLevel: Int = 1,
    val levelMinPoints: Float = 0f,
    val levelMaxPoints: Float = 30f,
    val progressToNextLevel: Float = 0f,  // 0..1
    val progressLabel: String = "0/30",
    val motivationQuote: String = "",
    val categories: List<CategoryProgress> = emptyList(),
    val todayDoneCount: Int = 0,
    val todayTotalCount: Int = 0,
    val weekDoneCount: Int = 0,
    val weekTotalCount: Int = 0
)

data class CategoryProgress(
    val id: Long,
    val name: String,
    val emoji: String,
    val points: Float,
    val level: Int,
    val habits: List<HabitPointsUi> = emptyList()
)

data class HabitPointsUi(
    val habitId: Long,
    val habitName: String,
    val points: Float
)