package ru.nikita.yourupgrade.data.calculator

import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import kotlin.math.roundToInt

object PointsCalculator {

    private val LEVEL_THRESHOLDS = listOf(0f, 30f, 100f, 250f, 500f, 1000f)

    fun calculatePoints(
        completionMode: CompletionMode,
        completedValue: Float,
        targetValue: Float,
        habitType: HabitType
    ): Float {
        return when (habitType) {
            HabitType.DAILY -> calculateDailyPoints(completedValue, targetValue, completionMode)
            HabitType.WEEKLY -> calculateWeeklyPoints(completedValue, targetValue, completionMode)
        }
    }

    private fun calculateDailyPoints(
        completedValue: Float,
        targetValue: Float,
        completionMode: CompletionMode
    ): Float {
        return when (completionMode) {
            CompletionMode.CHECKABLE -> {
                if (completedValue >= 1f) 1.0f else 0f
            }

            CompletionMode.MEASURABLE -> {
                val percent = if (targetValue > 0) completedValue / targetValue else 0f
                when {
                    percent >= 1.0f -> 1.0f
                    percent >= 0.5f -> (percent * 100).roundToInt() / 100f  // пропорционально 0.5-1
                    else -> -0.25f
                }
            }
        }
    }

    private fun calculateWeeklyPoints(
        completedValue: Float,
        targetValue: Float,
        completionMode: CompletionMode
    ): Float {
        return when (completionMode) {
            CompletionMode.CHECKABLE -> {
                // Отмечаемая: 2 балла за выполнение
                if (completedValue >= 1f) 2.0f else 0f
            }

            CompletionMode.MEASURABLE -> {
                val percent = if (targetValue > 0) completedValue / targetValue else 0f
                when {
                    percent >= 1.0f -> 2.0f
                    percent >= 0.5f -> (percent * 200).roundToInt() / 100f  // пропорционально 1-2
                    else -> -0.5f
                }
            }
        }
    }

    /** Определяет текущий уровень по сумме баллов */
    fun getLevel(totalPoints: Float): Int {
        val safePoints = totalPoints.coerceAtLeast(0f)
        return when {
            safePoints < LEVEL_THRESHOLDS[1] -> 1
            safePoints < LEVEL_THRESHOLDS[2] -> 2
            safePoints < LEVEL_THRESHOLDS[3] -> 3
            safePoints < LEVEL_THRESHOLDS[4] -> 4
            safePoints < LEVEL_THRESHOLDS[5] -> 5
            else -> 5
        }
    }

    /** Нижняя граница текущего уровня */
    fun getLevelMinPoints(level: Int): Float {
        return LEVEL_THRESHOLDS.getOrElse(level - 1) { 0f }
    }

    /** Верхняя граница текущего уровня (для прогресс-бара) */
    fun getLevelMaxPoints(level: Int): Float {
        return LEVEL_THRESHOLDS.getOrElse(level) { 1000f }
    }

    /** Мотивационная фраза в зависимости от уровня */
    fun getMotivationQuote(level: Int): String {
        return when (level) {
            1 -> "Каждый шаг вперёд — это победа!"
            2 -> "Ты уже в пути, не останавливайся!"
            3 -> "Привычки становятся частью вас!"
            4 -> "Мастерство требует постоянства!"
            5 -> "Ты на вершине, но нет предела совершенства!"
            else -> "Начните свой путь"
        }
    }
}