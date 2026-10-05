package ru.nikita.yourupgrade.data.calculator

import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import kotlin.math.roundToInt

object PointsCalculator {

    private val LEVEL_THRESHOLDS = listOf(0f, 30f, 100f, 250f, 500f, 1000f)

    /**
     * Рассчитывает баллы за выполнение привычки
     */

    /* fun calculatePointsFromLog(log: HabitLogEntry, completionMode: CompletionMode): Float {
         return calculatePoints(
             completedValue = log.completedValue,
             targetValue = log.targetValue,
             completionMode = completionMode
         )
     }*/


    // Старая версия для новых записей
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
                    percent >= 0.5f -> 0.5f
                    else -> -0.25f       // Штраф за пропуск
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
                // Измеряемая: пропорционально
                val percent = if (targetValue > 0) completedValue / targetValue else 0f
                when {
                    percent >= 1.0f -> 2.0f      // 100% → 2 балла
                    percent >= 0.5f -> 1.0f      // 50-99% → 1 балл
                    else -> -0.25f              // Штраф за пропуск
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

    /**
     * Верхняя граница текущего уровня (для прогресс-бара)
     */
    fun getLevelMaxPoints(level: Int): Float {
        return LEVEL_THRESHOLDS.getOrElse(level) { 1000f }
    }

    /**
     * Мотивационная фраза в зависимости от уровня
     */
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

    //todo в Utils
    private fun roundTo2Decimals(value: Float): Float {
        return ((value * 100f).roundToInt() / 100f)
    }
}