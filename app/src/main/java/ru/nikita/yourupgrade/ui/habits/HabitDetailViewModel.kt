package ru.nikita.yourupgrade.ui.habits


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _habitId = MutableStateFlow<Long>(-1L)

    private val db = AppDatabase.getDatabase(application)

    val habit: StateFlow<HabitEntity?> = _habitId.flatMapLatest { id ->
        if (id == -1L) flowOf(null)
        else db.habitDao().getHabitByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Логи за текущую неделю
    val weekLogs: StateFlow<List<HabitLogEntry>> = _habitId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList())
        else {
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            val sunday = monday.plusDays(6)
            repository.getLogsForHabitInRange(
                id,
                monday.format(dateFormatter),
                sunday.format(dateFormatter)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository = HabitRepository(db.habitDao(), db.categoryDao(), db.habitLogDao())
    }

    fun setHabitId(id: Long) {
        _habitId.value = id
    }

    // 🆕 Строим список дней недели со статусами
    fun buildWeekStatuses(habit: HabitEntity, logs: List<HabitLogEntry>): List<DayStatusUiItem> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val today = LocalDate.now()
        val dayNames = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

        val logMap = logs.associateBy { it.date }

        return (0..6).map { index ->
            val date = monday.plusDays(index.toLong())
            val dateStr = date.format(dateFormatter)
            val log = logMap[dateStr]

            DayStatusUiItem(
                dayShortName = dayNames[index],
                date = dateStr,
                isDone = log != null,
                completedValue = log?.completedValue ?: 0f,
                targetValue = habit.targetValue,
                isToday = date == today,
                completionMode = habit.completionMode
            )
        }
    }
}