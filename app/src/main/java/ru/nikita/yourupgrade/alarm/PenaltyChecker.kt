package ru.nikita.yourupgrade.alarm

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.data.model.LogStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PenaltyChecker {

    private const val PENALTY_POINTS = -0.25f
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Проверяет вчерашний день и начисляет штрафы за невыполненные привычки.
     * Вызывается при открытии приложения.
     */
    fun checkAndApplyPenalties(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val yesterday = LocalDate.now().minusDays(1)
            val yesterdayStr = yesterday.format(dateFormatter)

            // 1. Получаем все активные ежедневные привычки
            val dailyHabits = db.habitDao()
                .getActiveHabitsByType(HabitType.DAILY)
                .first()

            if (dailyHabits.isEmpty()) return@launch

            // 2. Получаем все логи за вчера
            val yesterdayLogs = db.habitLogDao()
                .getLogsForDate(yesterdayStr)
                .first()

            val logMap = yesterdayLogs.associateBy { it.habitId }

            // 3. Проверяем каждую привычку
            for (habit in dailyHabits) {

                // 🆕 ПРОВЕРЯЕМ ДАТУ СОЗДАНИЯ ПРИВЫЧКИ
                val createdDate = Instant.ofEpochMilli(habit.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                // Если привычка создана вчера или позже — пропускаем
                if (createdDate >= yesterday) {
                    continue
                }

                val log = logMap[habit.id]

                val shouldPenalize = when {
                    // Нет записи вообще → штраф
                    log == null -> true

                    // Измеряемая привычка с выполнением < 50% → штраф
                    habit.completionMode == CompletionMode.MEASURABLE -> {
                        val percent = if (habit.targetValue > 0) {
                            log.completedValue / habit.targetValue
                        } else 0f
                        percent < 0.5f
                    }

                    // Галочная с записью → ок, штрафа нет
                    else -> false
                }

                if (shouldPenalize) {
                    // Проверяем, не был ли штраф уже начислен за вчера
                    val alreadyPenalized = yesterdayLogs.any {
                        it.habitId == habit.id && it.status == LogStatus.MISSED
                    }

                    if (!alreadyPenalized) {
                        val penaltyLog = HabitLogEntry(
                            habitId = habit.id,
                            date = yesterdayStr,  // 🆕 вчерашним днём!
                            completedValue = 0f,
                            earnedPoints = PENALTY_POINTS,
                            status = LogStatus.MISSED
                        )
                        db.habitLogDao().insertLog(penaltyLog)
                    }
                }
            }
        }
    }
}