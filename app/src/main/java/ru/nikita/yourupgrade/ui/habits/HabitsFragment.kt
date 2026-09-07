package ru.nikita.yourupgrade.ui.habits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.databinding.FragmentHabitsBinding
import java.util.concurrent.TimeUnit
import androidx.navigation.fragment.findNavController
import ru.nikita.yourupgrade.R  // ← проверьте импорт!

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HabitsViewModel by viewModels()
    private lateinit var dailyAdapter: HabitAdapter
    private lateinit var weeklyAdapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupToggleGroup()
        setupFab()
        observeData()
    }

    private fun setupAdapters() {
        dailyAdapter = HabitAdapter(
            onItemClick = { habit ->
                // 🎯 Переход на детали — используем ID destination напрямую
                val bundle = Bundle().apply {
                    putLong("habitId", habit.id)
                }
                findNavController().navigate(
                    R.id.habitDetailFragment,
                    bundle
                )
            },
            onDeleteClick = { viewModel.deleteHabit(it.id) },
            onMarkDone = { habit ->
                viewModel.toggleHabitCompletion(habit.id)
                launchConfetti()  // 🆕 Конфетти при отметке!
            },
            onUnmarkDone = { habit ->
                viewModel.toggleHabitCompletion(habit.id)
                // Без конфетти при снятии
            },
            onProgressClick = { habit -> showInputValueDialog(habit) }
        )
        weeklyAdapter = HabitAdapter(
            onItemClick = { habit ->
                // 🆕 Переход на экран деталей
                val bundle = Bundle().apply {
                    putLong("habitId", habit.id)
                }
                findNavController().navigate(
                    R.id.habitDetailFragment,
                    bundle
                )
            },
            onDeleteClick = { viewModel.deleteHabit(it.id) },
            onMarkDone = { /* для еженедельных позже */ },
            onUnmarkDone = { /* для еженедельных позже */ },
            onProgressClick = { /* для еженедельных позже */ }
        )

        binding.rvDailyHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDailyHabits.adapter = dailyAdapter

        binding.rvWeeklyHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWeeklyHabits.adapter = weeklyAdapter
    }

    // 🆕 Конфетти!
    private fun launchConfetti() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x4CAF50),
            emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(80),
            position = Position.Relative(
                x = 0.5,
                y = 0.3
            )
        )

        binding.konfettiView.start(party)
    }


    private fun showInputValueDialog(habit: HabitUiItem) {
        val dialog = InputValueDialogFragment().apply {
            this.habitName = habit.name
            this.targetValue = habit.targetValue
            this.currentValue = habit.completedValue

            this.onValueEntered = { value ->
                viewModel.saveMeasurableValue(habit.id, value)
            }
        }
        dialog.show(childFragmentManager, "input_value")
    }

    private fun setupToggleGroup() {
        // По умолчанию активны ежедневные
        binding.btnDaily.isChecked = true

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnDaily.id -> showDaily()
                    binding.btnWeekly.id -> showWeekly()
                }
            }
        }
    }

    private fun showDaily() {
        binding.dailyFrame.visibility = View.VISIBLE
        binding.weeklyFrame.visibility = View.GONE
    }

    private fun showWeekly() {
        binding.dailyFrame.visibility = View.GONE
        binding.weeklyFrame.visibility = View.VISIBLE
    }

    private fun setupFab() {
        binding.fabAddHabit.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialog = AddHabitDialogFragment()
        dialog.onHabitCreated = { name, categoryId, type, completionMode, target ->
            viewModel.addHabit(name, categoryId, type, completionMode, target)
        }
        dialog.show(childFragmentManager, "add_habit")
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    combine(
                        viewModel.dailyHabits,
                        viewModel.categories,
                        viewModel.logsForDate,
                        ::toUiItemsWithStatus
                    ).collect { uiItems ->
                        dailyAdapter.submitList(uiItems)
                        binding.tvEmptyDaily.visibility =
                            if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    combine(
                        viewModel.weeklyHabits,
                        viewModel.categories,
                        viewModel.logsForDate,
                        ::toUiItemsWithStatus
                    ).collect { uiItems ->
                        weeklyAdapter.submitList(uiItems)
                        binding.tvEmptyWeekly.visibility =
                            if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun toUiItemsWithStatus(
        habits: List<HabitEntity>,
        categories: List<SkillCategoryEntity>,
        logs: List<HabitLogEntry>
    ): List<HabitUiItem> {
        val categoryMap = categories.associateBy { it.id }
        val logMap = logs.associateBy { it.habitId }

        val items = habits.map { habit ->
            val category = categoryMap[habit.categoryId]
            val log = logMap[habit.id]

            val isDone = log != null
            val completedValue = when (habit.completionMode) {
                CompletionMode.CHECKABLE -> if (isDone) 1f else 0f
                CompletionMode.MEASURABLE -> log?.completedValue ?: 0f
            }

            HabitUiItem(
                id = habit.id,
                name = habit.name,
                categoryName = category?.name ?: "Без категории",
                categoryEmoji = category?.iconEmoji ?: "📌",
                type = habit.type,
                completionMode = habit.completionMode,
                targetValue = habit.targetValue,
                isDoneToday = isDone,
                completedValue = completedValue
            )
        }

        return items.sortedWith(
            compareBy<HabitUiItem> { it.isFullyDone }
                .thenBy { it.name.lowercase() }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}