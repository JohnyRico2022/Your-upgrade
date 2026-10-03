package ru.nikita.yourupgrade.ui.history

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.databinding.FragmentHistoryBinding
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeekStatsViewModel by viewModels()
    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggle()
        setupDayLabels()
        setupBarChart()
        setupSwipeGestures()
        observeState()
    }

    private fun setupToggle() {
        binding.buttonWeek.isChecked = true

        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && checkedId == binding.buttonMonth.id) {
                Toast.makeText(requireContext(), "Месяц: скоро будет", Toast.LENGTH_SHORT).show()
                binding.buttonWeek.isChecked = true
            }
        }
    }

    private fun setupDayLabels() {
        val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        binding.dayLabels.removeAllViews()

        dayNames.forEach { name ->
            val textView = TextView(requireContext()).apply {
                text = name
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            binding.dayLabels.addView(textView)
        }
    }

    private fun setupBarChart() {
        binding.barChart.setOnDayClickListener { index ->
            viewModel.selectDay(index)
        }
    }

    // 🆕 Настройка свайпов
    private fun setupSwipeGestures() {
        gestureDetector =
            GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {

                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 100

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Свайп вправо → предыдущая неделя
                                viewModel.previousWeek()
                            } else {
                                // Свайп влево → следующая неделя
                                viewModel.nextWeek()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

        // Применяем GestureDetector к карточке заголовка недели
        binding.cardWeekHeader.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    // Заполняем числа баллов под столбиками
    private fun fillNumbers(days: List<DayStats>) {
        binding.numbersContainer.removeAllViews()

        days.forEach { day ->
            val hasAnyData = day.habits.isNotEmpty() &&
                    day.habits.any { it.status != DayHabitStatus.NOT_CREATED }

            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = android.view.Gravity.CENTER
                textSize = 12f

                if (!hasAnyData) {
                    // День без данных (привычки ещё не были созданы)
                    text = "—"
                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.darker_gray
                        )
                    )
                } else {
                    // Форматируем число с двумя знаками
                    val pointsText = if (day.points >= 0) {
                        String.format(Locale.getDefault(), "+%.2f", day.points)
                    } else {
                        String.format(Locale.getDefault(), "%.2f", day.points)
                    }
                    text = pointsText

                    // Цвет по знаку
                    val color = when {
                        day.points > 0 -> 0xFF4CAF50.toInt()   // Зелёный
                        day.points < 0 -> 0xFFE53935.toInt()   // Красный
                        else -> 0xFF9E9E9E.toInt()              // Серый (ровно 0)
                    }
                    setTextColor(color)
                }
            }
            binding.numbersContainer.addView(tv)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvWeekTitle.text = "Неделя ${state.weekNumber}"
                    binding.tvWeekRange.text = state.dateRangeLabel

                    val hasAnyData = state.days.any { day ->
                        day.habits.isNotEmpty() && day.habits.any { it.status != DayHabitStatus.NOT_CREATED }
                    }

                    if (hasAnyData) {
                        // Показываем диаграмму
                        binding.chartContainer.visibility = View.VISIBLE
                        binding.tvNoData.visibility = View.GONE

                        val points = state.days.map { it.points }
                        binding.barChart.setData(points, state.selectedDayIndex)

                        // 🆕 Заполняем числа под столбиками
                        fillNumbers(state.days)
                    } else {
                        // Показываем баннер
                        binding.chartContainer.visibility = View.GONE
                        binding.tvNoData.visibility = View.VISIBLE
                    }


                    val points = state.days.map { it.points }
                    binding.barChart.setData(points, state.selectedDayIndex)

                    /* val totalText = if (state.totalPoints >= 0) {
                         "+${String.format(Locale.getDefault(), "%.2f", state.totalPoints)}"
                     } else {
                         String.format(Locale.getDefault(), "%.2f", state.totalPoints)
                     }*/


                    binding.tvDailyPoints.text = formatPoints(state.dailyHabitsPoints)
                    binding.tvWeeklyPoints.text = formatPoints(state.weeklyHabitsPoints)
                    binding.tvAchievementPoints.text = "+${state.achievementPoints}"
                    binding.tvWeekTotal.text = formatPoints(state.totalPoints)

                    val totalColor = if (state.totalPoints >= 0) {
                        0xFF4CAF50.toInt()
                    } else {
                        0xFFE53935.toInt()
                    }
                    binding.tvWeekTotal.setTextColor(totalColor)

                    // Детали выбранного дня
                    val selectedDay = state.selectedDay
                    if (selectedDay != null) {
                        binding.cardDayDetails.visibility = View.VISIBLE

                        val dayFormatter =
                            java.time.format.DateTimeFormatter.ofPattern("EEEE, dd.MM")
                        binding.tvDayDetailsTitle.text = selectedDay.date.format(dayFormatter)

                        fillDayDetails(selectedDay.habits)
                    } else {
                        binding.cardDayDetails.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun formatPoints(points: Float): String {
        return if (points >= 0) {
            "+${String.format(Locale.getDefault(), "%.2f", points)}"
        } else {
            String.format(Locale.getDefault(), "%.2f", points)
        }
    }

    private fun fillDayDetails(habits: List<HabitDayStatus>) {
        binding.dayDetailsContainer.removeAllViews()

        habits.forEach { habit ->
            val habitView = createHabitRow(habit)
            binding.dayDetailsContainer.addView(habitView)
        }
    }

    private fun createHabitRow(habit: HabitDayStatus): View {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val (icon, color) = when (habit.status) {
            DayHabitStatus.DONE -> "✔️" to 0xFF4CAF50.toInt()
            DayHabitStatus.MISSED -> "❌" to 0xFFE53935.toInt()
            DayHabitStatus.NOT_CREATED -> "➖" to 0xFF9E9E9E.toInt()
        }

        val statusView = TextView(context).apply {
            text = icon
            textSize = 16f
            layoutParams =
                LinearLayout.LayoutParams(dpToPx(32), ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.CENTER
        }

        val nameView = TextView(context).apply {
            text = "${habit.categoryEmoji} ${habit.habitName}"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(12)
            }
        }

        val pointsText = when {
            habit.status == DayHabitStatus.NOT_CREATED -> ""
            habit.earnedPoints >= 0 -> "+${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    habit.earnedPoints
                )
            }"

            else -> String.format(Locale.getDefault(), "%.2f", habit.earnedPoints)
        }

        val pointsView = TextView(context).apply {
            text = pointsText
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }

        container.addView(statusView)
        container.addView(nameView)
        container.addView(pointsView)

        return container
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}