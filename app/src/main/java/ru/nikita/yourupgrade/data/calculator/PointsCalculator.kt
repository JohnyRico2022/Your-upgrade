package ru.nikita.yourupgrade.data.calculator

import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.model.CompletionMode
import kotlin.math.roundToInt

object PointsCalculator {

    private val LEVEL_THRESHOLDS = listOf(0f, 30f, 100f, 250f, 500f, 1000f)

    /**
     * Рассчитывает баллы за выполнение привычки
     */

    // 🆕 Новая версия: принимает цель из лога
    fun calculatePointsFromLog(log: HabitLogEntry, completionMode: CompletionMode): Float {
        return calculatePoints(
            completedValue = log.completedValue,
            targetValue = log.targetValue,
            completionMode = completionMode
        )
    }


    // Старая версия для новых записей
    fun calculatePoints(
        completionMode: CompletionMode,
        completedValue: Float,
        targetValue: Float
    ): Float {
        return when (completionMode) {
            CompletionMode.CHECKABLE -> {
                // Отмечаемая: 1.0 балл если выполнено
                if (completedValue > 0f) 1.0f else 0f
            }

            CompletionMode.MEASURABLE -> {
                // Измеряемая: пропорционально, но минимум 50% для зачёта
                if (targetValue <= 0f) return 0f

                val percent = completedValue / targetValue

                if (percent < 0.5f) {
                    0f  // менее 50% — не засчитываем
                } else {
                    // от 50% до 100%: шкала 0.5 → 1.0
                    // формула: 0.5 + 0.5 * (percent)
                    val rawPoints = 0.5f + 0.5f * percent
                    // Ограничиваем сверху 1.0 (если перевыполнил — больше не даём пока)
                    val capped = rawPoints.coerceAtMost(1.0f)
                    // Округление до 2 знаков
                    roundTo2Decimals(capped)
                }
            }
        }
    }

    /**
     * Определяет текущий уровень по сумме баллов
     */
    fun getLevel(totalPoints: Float): Int {
        val safePoints = totalPoints.coerceAtLeast(0f)  // 🆕 не ниже нуля
        return when {
            safePoints < LEVEL_THRESHOLDS[1] -> 1
            safePoints < LEVEL_THRESHOLDS[2] -> 2
            safePoints < LEVEL_THRESHOLDS[3] -> 3
            safePoints < LEVEL_THRESHOLDS[4] -> 4
            safePoints < LEVEL_THRESHOLDS[5] -> 5
            else -> 5
        }
    }

    /**
     * Нижняя граница текущего уровня
     */
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
            2 -> "Вы уже в пути, не останавливайтесь!"
            3 -> "Привычки становятся частью вас!"
            4 -> "Мастерство требует постоянства!"
            5 -> "Вы достигли вершины, но нет предела совершенства!"
            else -> "Начните свой путь"
        }
    }

    private fun roundTo2Decimals(value: Float): Float {
        return ((value * 100f).roundToInt() / 100f)
    }
}