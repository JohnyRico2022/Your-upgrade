package ru.nikita.yourupgrade.ui.rewards

import ru.nikita.yourupgrade.data.model.AchievementType

data class AchievementLevelUi(
    val level: Int,
    val threshold: Int,
    val points: Int,
    val isEarned: Boolean,
    val earnedDate: String? = null,
    val currentProgress: Int = 0
)

data class AchievementCategoryUi(
    val type: AchievementType,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val levels: List<AchievementLevelUi>
)

data class RewardsUiState(
    val categories: List<AchievementCategoryUi> = emptyList(),
    val totalAchievementPoints: Int = 0,
    val hasEnoughHabits: Boolean = true,  // 🆕 Достаточно ли привычек
    val habitsCount: Int = 0               // 🆕 Текущее количество привычек
)
