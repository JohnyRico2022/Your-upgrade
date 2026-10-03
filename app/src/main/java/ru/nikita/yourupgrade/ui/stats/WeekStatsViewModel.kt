package ru.nikita.yourupgrade.ui.stats

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.model.CompletionMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import ru.nikita.yourupgrade.data.model.HabitType

class WeekStatsViewModel(application: Application) : AndroidViewModel(application) {

    //todo нужно через репозиторий
    private val db = AppDatabase.getDatabase(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 🆕 По умолчанию показываем ПРЕДЫДУЩУЮ неделю
    private val _weekStart = MutableStateFlow(
        LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1)
    )

    private val _uiState = MutableStateFlow(WeekStatsUiState())
    val uiState: StateFlow<WeekStatsUiState> = _uiState.asStateFlow()

    init {
        loadWeekStats()
    }

    // 🆕 Переход на предыдущую неделю
    fun previousWeek() {
        _weekStart.value = _weekStart.value.minusWeeks(1)
        loadWeekStats()
    }

    // 🆕 Переход на следующую неделю (но не дальше предыдущей от текущей)
    fun nextWeek() {
        val lastAllowedWeek = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1)
        val nextWeek = _weekStart.value.plusWeeks(1)

        if (!nextWeek.isAfter(lastAllowedWeek)) {
            _weekStart.value = nextWeek
            loadWeekStats()
        }
    }

    // 🆕 Можно ли перейти на следующую неделю
    fun canGoNext(): Boolean {
        val lastAllowedWeek = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1)
        return _weekStart.value.isBefore(lastAllowedWeek)
    }

    // 🆕 Выбор дня
    fun selectDay(index: Int) {
        Log.d("MyLog", "selectDay: $index")
        _uiState.value = _uiState.value.copy(selectedDayIndex = index)
    }

    // 🆕 Загрузка статистики за неделю
    private fun loadWeekStats() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                buildWeekStats(_weekStart.value)
            }
            _uiState.value = state
        }
    }

    private suspend fun buildWeekStats(weekStart: LocalDate): WeekStatsUiState {
        val weekEnd = weekStart.plusDays(6)

        // Номер недели (ISO, понедельник — первый день)
        val weekFields = WeekFields.ISO
        val weekNumber = weekStart.get(weekFields.weekOfWeekBasedYear())

        // Получаем все привычки
        val allHabits = db.habitDao().getAllHabitsSync()
        val dailyHabits = allHabits.filter { it.type == HabitType.DAILY && it.isActive }

        // Получаем категории
        val categories = db.categoryDao().getAllCategoriesSync()
        val categoryMap = categories.associateBy { it.id }

        // Получаем все логи за неделю одним запросом
        val weekStartStr = weekStart.format(dateFormatter)
        val weekEndStr = weekEnd.format(dateFormatter)
        val weekLogs = db.habitLogDao().getLogsForDateRange(weekStartStr, weekEndStr)

        val logsByDate = weekLogs.groupBy { it.date }

        // Строим статистику по дням
        val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val days = (0..6).map { index ->
            val date = weekStart.plusDays(index.toLong())
            val dateStr = date.format(dateFormatter)
            val dayLogs = logsByDate[dateStr] ?: emptyList()

            android.util.Log.d("MyLog", "--- День: $dateStr, логов: ${dayLogs.size} ---")

            // Считаем баллы за день (включая штрафы)
            val dayPoints = dayLogs.sumOf { it.earnedPoints.toDouble() }.toFloat()

            // Статус каждой привычки
            val habitStatuses = dailyHabits.map { habit ->
                val createdDate = java.time.Instant.ofEpochMilli(habit.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()

                val habitLog = dayLogs.find { it.habitId == habit.id }

                val status = when {
                    // Привычка ещё не была создана
                    createdDate.isAfter(date) -> DayHabitStatus.NOT_CREATED

                    // Есть запись о штрафе или нет записи вообще
                    habitLog == null -> DayHabitStatus.MISSED
                    habitLog.earnedPoints < 0 -> DayHabitStatus.MISSED

                    // Измеряемая < 50%
                    habit.completionMode == CompletionMode.MEASURABLE -> {
                        val percent = if (habit.targetValue > 0) {
                            habitLog.completedValue / habit.targetValue
                        } else 0f
                        if (percent >= 0.5f) DayHabitStatus.DONE else DayHabitStatus.MISSED
                    }

                    // Галочная с записью
                    else -> DayHabitStatus.DONE
                }

                HabitDayStatus(
                    habitId = habit.id,
                    habitName = habit.name,
                    categoryEmoji = categoryMap[habit.categoryId]?.iconEmoji ?: "📌",
                    status = status,
                    earnedPoints = habitLog?.earnedPoints ?: 0f
                )
            }

            DayStats(
                date = date,
                dayShortName = dayNames[index],
                points = dayPoints,
                habits = habitStatuses
            )
        }

        val totalPoints = days.sumOf { it.points.toDouble() }.toFloat()

        val dailyHabitsIds = allHabits.filter { it.type == HabitType.DAILY }.map { it.id }.toSet()
        val weeklyHabitsIds = allHabits.filter { it.type == HabitType.WEEKLY }.map { it.id }.toSet()

        var dailyPoints = 0f
        var weeklyPoints = 0f

        for (log in weekLogs) {
            when {
                log.habitId in dailyHabitsIds -> dailyPoints += log.earnedPoints
                log.habitId in weeklyHabitsIds -> weeklyPoints += log.earnedPoints
            }
        }

        val achievementPoints = db.achievementDao().getPointsForDateRange(weekStartStr, weekEndStr)

        return WeekStatsUiState(
            weekNumber = weekNumber,
            weekStart = weekStart,
            weekEnd = weekEnd,
            days = days,
            totalPoints = totalPoints,
            selectedDayIndex = -1,
            dailyHabitsPoints = dailyPoints,
            weeklyHabitsPoints = weeklyPoints,
            achievementPoints = achievementPoints
        )
    }
}