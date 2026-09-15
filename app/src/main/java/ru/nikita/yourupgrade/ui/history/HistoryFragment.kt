package ru.nikita.yourupgrade.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch
import android.widget.Toast
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeekStatsViewModel by viewModels()

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
        setupWeekNavigation()
        setupDayLabels()
        setupBarChart()
        observeState()
    }

    private fun setupToggle() {
        binding.btnWeek.isChecked = true

        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && checkedId == binding.btnMonth.id) {
                Toast.makeText(requireContext(), "Месяц: скоро будет", Toast.LENGTH_SHORT).show()
                binding.btnWeek.isChecked = true
            }
        }
    }

    private fun setupWeekNavigation() {
        binding.btnPrevWeek.setOnClickListener {
            viewModel.previousWeek()
        }

        binding.btnNextWeek.setOnClickListener {
            viewModel.nextWeek()
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
            android.util.Log.d("HistoryFragment", "Клик по дню: $index")
            viewModel.selectDay(index)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvWeekTitle.text = "Неделя ${state.weekNumber}"
                    binding.tvWeekRange.text = state.dateRangeLabel

                    binding.btnNextWeek.isEnabled = viewModel.canGoNext()
                    binding.btnNextWeek.alpha = if (viewModel.canGoNext()) 1f else 0.3f

                    val points = state.days.map { it.points }
                    binding.barChart.setData(points, state.selectedDayIndex)

                    val totalText = if (state.totalPoints >= 0) {
                        "+${String.format(Locale.getDefault(), "%.2f", state.totalPoints)}"
                    } else {
                        String.format(Locale.getDefault(), "%.2f", state.totalPoints)
                    }
                    binding.tvWeekTotal.text = totalText

                    val totalColor = if (state.totalPoints >= 0) {
                        0xFF4CAF50.toInt()
                    } else {
                        0xFFE53935.toInt()
                    }
                    binding.tvWeekTotal.setTextColor(totalColor)

                    // 🆕 Детали выбранного дня через LinearLayout
                    val selectedDay = state.selectedDay
                    if (selectedDay != null) {
                        binding.tvDayDetailsTitle.visibility = View.VISIBLE
                        binding.dayDetailsContainer.visibility = View.VISIBLE

                        val dayFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd.MM")
                        binding.tvDayDetailsTitle.text = selectedDay.date.format(dayFormatter)

                        // 🆕 Заполняем контейнер
                        fillDayDetails(selectedDay.habits)
                    } else {
                        binding.tvDayDetailsTitle.visibility = View.GONE
                        binding.dayDetailsContainer.visibility = View.GONE
                    }
                }
            }
        }
    }

    // 🆕 Заполняем список привычек за день
    private fun fillDayDetails(habits: List<HabitDayStatus>) {
        binding.dayDetailsContainer.removeAllViews()

        android.util.Log.d("HistoryFragment", "fillDayDetails: ${habits.size} привычек")

        habits.forEach { habit ->
            val habitView = createHabitRow(habit)
            binding.dayDetailsContainer.addView(habitView)
        }
    }

    // 🆕 Создаём строку для одной привычки
    private fun createHabitRow(habit: HabitDayStatus): View {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(
                dpToPx(12),
                dpToPx(12),
                dpToPx(12),
                dpToPx(12)
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Статус (✔️ / ❌ / ➖)
        val (icon, color) = when (habit.status) {
            DayHabitStatus.DONE -> "✔️" to 0xFF4CAF50.toInt()
            DayHabitStatus.MISSED -> "❌" to 0xFFE53935.toInt()
            DayHabitStatus.NOT_CREATED -> "➖" to 0xFF9E9E9E.toInt()
        }

        val statusView = TextView(context).apply {
            text = icon
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.CENTER
        }

        // Название привычки
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

        // Баллы
        val pointsText = when {
            habit.status == DayHabitStatus.NOT_CREATED -> ""
            habit.earnedPoints >= 0 -> "+${String.format(Locale.getDefault(), "%.2f", habit.earnedPoints)}"
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