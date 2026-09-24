package ru.nikita.yourupgrade.ui.habits_detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.LogStatus
import ru.nikita.yourupgrade.data.repository.HabitRepository
import ru.nikita.yourupgrade.ui.habits.DayCompletionStatus
import ru.nikita.yourupgrade.ui.habits.DayStatusUiItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

class HabitDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _habitId = MutableStateFlow<Long>(-1L)

    private val _weekOffset = MutableStateFlow(0)
    private val db = AppDatabase.getDatabase(application)

    val habit: StateFlow<HabitEntity?> = _habitId.flatMapLatest { id ->
        if (id == -1L) flowOf(null)
        else db.habitDao().getHabitByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // 🆕 Логи зависят от привычки И от смещения недели
    val weekLogs: StateFlow<List<HabitLogEntry>> = combine(_habitId, _weekOffset) { id, offset ->
        id to offset
    }.flatMapLatest { (id, offset) ->
        if (id == -1L) {
            flowOf(emptyList())
        } else {
            val monday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
            val sunday = monday.plusDays(6)
            repository.getLogsForHabitInRange(
                id,
                monday.format(dateFormatter),
                sunday.format(dateFormatter)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🆕 Текущее смещение для UI
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    init {
        repository = HabitRepository(db.habitDao(), db.categoryDao(), db.habitLogDao())
    }

    fun setHabitId(id: Long) {
        _habitId.value = id
    }

    // 🆕 Переход на предыдущую неделю (свайп вправо)
    fun previousWeek() {
        _weekOffset.value -= 1
    }

    // 🆕 Переход на следующую неделю (свайп влево), но не дальше текущей
    fun nextWeek() {
        if (_weekOffset.value < 0) {
            _weekOffset.value += 1
        }
    }

    // 🆕 Можно ли идти вперёд
    fun canGoNext(): Boolean = _weekOffset.value < 0

    // 🆕 Заголовок недели: "Текущая неделя" или "Неделя 37"
    fun getWeekTitle(): String {
        return if (_weekOffset.value == 0) {
            "Текущая неделя"
        } else {
            val monday =
                LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(_weekOffset.value.toLong())
            val weekNumber = monday.get(WeekFields.ISO.weekOfWeekBasedYear())
            "Неделя $weekNumber"
        }
    }

    fun buildWeekStatuses(habit: HabitEntity, logs: List<HabitLogEntry>): List<DayStatusUiItem> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val today = LocalDate.now()
        val dayNames = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val logMap = logs.associateBy { it.date }

        return (0..6).map { index ->
            val date = monday.plusDays(index.toLong())
            val dateStr = date.format(dateFormatter)
            val log = logMap[dateStr]

            val status = when {
                date.isBefore(createdDate) -> DayCompletionStatus.NOT_CREATED
                log == null -> DayCompletionStatus.MISSED
                log.status == LogStatus.MISSED -> DayCompletionStatus.MISSED
                log.earnedPoints < 0 -> DayCompletionStatus.MISSED
                habit.completionMode == CompletionMode.MEASURABLE -> {
                    val percent = if (habit.targetValue > 0) {
                        log.completedValue / habit.targetValue
                    } else 0f

                    when {
                        percent >= 1.0f -> DayCompletionStatus.DONE     // 100%+ зелёный
                        percent >= 0.5f -> DayCompletionStatus.PARTIAL  // 50-99% жёлтый
                        else -> DayCompletionStatus.MISSED              // <50% красный
                    }
                }

                else -> DayCompletionStatus.DONE
            }

            DayStatusUiItem(
                dayShortName = dayNames[index],
                date = dateStr,
                status = status,
                completedValue = log?.completedValue ?: 0f,
                targetValue = habit.targetValue,
                isToday = date == today,
                completionMode = habit.completionMode
            )
        }
    }

    fun renameHabit(newName: String) {
        viewModelScope.launch {
            val habit = habit.value ?: return@launch
            val updated = habit.copy(name = newName)
            repository.updateHabit(updated)
        }
    }
}