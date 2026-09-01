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
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.databinding.FragmentHabitsBinding

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
            onItemClick = { /* TODO: редактирование */ },
            onDeleteClick = { viewModel.deleteHabit(it.id) }
        )
        weeklyAdapter = HabitAdapter(
            onItemClick = { /* TODO: редактирование */ },
            onDeleteClick = { viewModel.deleteHabit(it.id) }
        )

        binding.rvDailyHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDailyHabits.adapter = dailyAdapter

        binding.rvWeeklyHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWeeklyHabits.adapter = weeklyAdapter
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

                // Daily habits
                launch {
                    combine(viewModel.dailyHabits, viewModel.categories, ::toUiItems)
                        .collect { uiItems ->
                            dailyAdapter.submitList(uiItems)
                            binding.tvEmptyDaily.visibility =
                                if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                            if (binding.btnDaily.isChecked && uiItems.isNotEmpty()) {
                                binding.tvEmptyDaily.visibility = View.GONE
                            }
                        }
                }

                // Weekly habits
                launch {
                    combine(viewModel.weeklyHabits, viewModel.categories, ::toUiItems)
                        .collect { uiItems ->
                            weeklyAdapter.submitList(uiItems)
                            binding.tvEmptyWeekly.visibility =
                                if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                            if (binding.btnWeekly.isChecked && uiItems.isNotEmpty()) {
                                binding.tvEmptyWeekly.visibility = View.GONE
                            }
                        }
                }
            }
        }
    }

    private fun toUiItems(
        habits: List<HabitEntity>,
        categories: List<SkillCategoryEntity>
    ): List<HabitUiItem> {
        val categoryMap = categories.associateBy { it.id }
        return habits.map { habit ->
            val category = categoryMap[habit.categoryId]
            HabitUiItem(
                id = habit.id,
                name = habit.name,
                categoryName = category?.name ?: "Без категории",
                categoryEmoji = category?.iconEmoji ?: "📌",
                type = habit.type,
                completionMode = habit.completionMode,
                targetValue = habit.targetValue
            )
        } as List<HabitUiItem>
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}