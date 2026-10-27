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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PenaltyChecker {

    private const val DAILY_PENALTY = -0.25f
    private const val WEEKLY_PENALTY = -0.50f
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun checkAndApplyPenalties(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val allHabits = db.habitDao().getActiveHabits().first()
            val allLogs = db.habitLogDao().getAllLogs().first()
            val logsByHabit = allLogs.groupBy { it.habitId }

            // Ежедневные: backfill по дням
            val dailyHabits = allHabits.filter { it.type == HabitType.DAILY }
            for (habit in dailyHabits) {
                backfillDailyPenalties(
                    habit = habit,
                    habitLogs = logsByHabit[habit.id] ?: emptyList(),
                    yesterday = yesterday,
                    db = db
                )
            }

            // 🆕 Еженедельные: backfill по неделям
            val weeklyHabits = allHabits.filter { it.type == HabitType.WEEKLY }
            for (habit in weeklyHabits) {
                backfillWeeklyPenalties(
                    habit = habit,
                    habitLogs = logsByHabit[habit.id] ?: emptyList(),
                    today = today,
                    db = db
                )
            }
        }
    }

    /**
     * Ежедневные: штраф за каждый пропущенный день.
     */
    private suspend fun backfillDailyPenalties(
        habit: ru.nikita.yourupgrade.data.local.entity.HabitEntity,
        habitLogs: List<HabitLogEntry>,
        yesterday: LocalDate,
        db: AppDatabase
    ) {
        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val lastLogDate = habitLogs.maxByOrNull { it.date }?.let { log ->
            LocalDate.parse(log.date, dateFormatter)
        } ?: createdDate.minusDays(1)

        val startDate = maxOf(lastLogDate.plusDays(1), createdDate)
        var checkDate = startDate
        val logsMap = habitLogs.associateBy { it.date }

        while (!checkDate.isAfter(yesterday)) {
            val dateStr = checkDate.format(dateFormatter)
            val existingLog = logsMap[dateStr]

            if (existingLog == null) {
                // Нет записи — штраф
                val penaltyLog = HabitLogEntry(
                    habitId = habit.id,
                    date = dateStr,
                    completedValue = 0f,
                    earnedPoints = DAILY_PENALTY,
                    status = LogStatus.MISSED,
                    targetValue = habit.targetValue
                )
                db.habitLogDao().insertLog(penaltyLog)
            } else if (habit.completionMode == CompletionMode.MEASURABLE &&
                existingLog.status != LogStatus.MISSED
            ) {
                val percent = if (habit.targetValue > 0) {
                    existingLog.completedValue / habit.targetValue
                } else 0f

                if (percent < 0.5f) {
                    val penaltyLog = existingLog.copy(
                        earnedPoints = DAILY_PENALTY,
                        status = LogStatus.MISSED
                    )
                    db.habitLogDao().insertLog(penaltyLog)
                }
            }

            checkDate = checkDate.plusDays(1)
        }
    }

    /**
     * 🆕 Еженедельные: штраф за каждую завершённую неделю с прогрессом < 50%.
     */
    private suspend fun backfillWeeklyPenalties(
        habit: ru.nikita.yourupgrade.data.local.entity.HabitEntity,
        habitLogs: List<HabitLogEntry>,
        today: LocalDate,
        db: AppDatabase
    ) {
        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Начало недели создания привычки
        val firstWeekStart = createdDate.with(DayOfWeek.MONDAY)

        // Понедельник текущей недели (неделя ещё идёт — её не проверяем)
        val currentWeekStart = today.with(DayOfWeek.MONDAY)

        // Группируем логи по неделям
        val logsByWeek = habitLogs.groupBy { log ->
            LocalDate.parse(log.date, dateFormatter).with(DayOfWeek.MONDAY)
        }

        // Последняя обработанная неделя (по логам)
        val lastProcessedWeek = logsByWeek.keys.maxOrNull()
            ?: firstWeekStart.minusWeeks(1)

        // Начинаем со следующей недели после последней обработанной
        var weekStart = lastProcessedWeek.plusWeeks(1)

        while (weekStart.isBefore(currentWeekStart)) {
            val weekLogs = logsByWeek[weekStart] ?: emptyList()

            // Проверяем: была ли привычка создана до конца этой недели
            val weekEnd = weekStart.plusDays(6)
            if (createdDate.isAfter(weekEnd)) {
                weekStart = weekStart.plusWeeks(1)
                continue
            }

            val weekTotal = weekLogs.sumOf { it.completedValue.toDouble() }.toFloat()
            val percent = if (habit.targetValue > 0) {
                weekTotal / habit.targetValue
            } else 0f

            // Штраф если < 50%
            if (percent < 0.5f) {
                // Проверяем, нет ли уже штрафа за эту неделю
                val hasPenalty = weekLogs.any { it.status == LogStatus.MISSED }

                if (!hasPenalty) {
                    // Привязываем штраф к воскресенью недели
                    val penaltyDate = weekEnd.format(dateFormatter)
                    val penaltyLog = HabitLogEntry(
                        habitId = habit.id,
                        date = penaltyDate,
                        completedValue = weekTotal,  // сохраняем реальный прогресс
                        earnedPoints = WEEKLY_PENALTY,
                        status = LogStatus.MISSED,
                        targetValue = habit.targetValue
                    )
                    db.habitLogDao().insertLog(penaltyLog)
                }
            }

            weekStart = weekStart.plusWeeks(1)
        }
    }
}