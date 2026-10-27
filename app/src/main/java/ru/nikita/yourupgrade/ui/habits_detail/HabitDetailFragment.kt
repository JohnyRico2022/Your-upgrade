package ru.nikita.yourupgrade.ui.habits_detail

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.databinding.FragmentHabitDetailBinding
import ru.nikita.yourupgrade.ui.habits.DayCompletionStatus

class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HabitDetailViewModel by viewModels()
    private lateinit var dayAdapter: DayStatusAdapter
    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //todo вынести в ключи
        val habitId = arguments?.getLong("habitId", -1L) ?: -1L
        if (habitId == -1L) {
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.setHabitId(habitId)

        setupWeekRecyclerView()
        setupNavigationButtons()
        observeData()
    }

    private fun setupWeekRecyclerView() {
        dayAdapter = DayStatusAdapter()
        binding.rvWeekDays.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvWeekDays.adapter = dayAdapter
    }

    private fun setupNavigationButtons() {
        binding.arrowLeft.setOnClickListener {
            viewModel.previousWeek()
        }
        binding.arrowRight.setOnClickListener {
            viewModel.nextWeek()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.habit,
                    viewModel.weekLogs,
                    viewModel.weekOffset
                ) { habit, logs, offset ->
                    if (habit != null) {
                        /** Заголовок */
                        binding.tvHabitName.text = habit.name

                        binding.btnMenu.setOnClickListener {
                            showHabitMenu()
                        }

                        /** Диапазон дат */
                        binding.tvWeekRange.text = viewModel.getWeekRangeLabel()

                        /** Категория */
                        val typeLabel = when (habit.type) {
                            HabitType.DAILY -> getString(R.string.habit_detail_type_daily)
                            HabitType.WEEKLY -> getString(R.string.habit_detail_type_weekly)
                        }
                        binding.tvCategoryInfo.text = typeLabel

                        /** Цель */
                        binding.tvTargetInfo.text = when (habit.completionMode) {
                            CompletionMode.MEASURABLE -> getString(
                                R.string.habit_detail_mode_measurable_format,
                                habit.targetValue.toLong(),
                                habit.unit
                            )

                            CompletionMode.CHECKABLE -> getString(R.string.habit_detail_mode_checkable)
                        }

                        /** Заголовок недели */
                        binding.tvWeekTitle.text = viewModel.getWeekTitle()

                        /** Блокировка кнопки вперёд на текущей неделе */
                        val isCurrentWeek = offset == 0
                        binding.arrowRight.isEnabled = !isCurrentWeek
                        binding.arrowRight.alpha = if (isCurrentWeek) 0.3f else 1.0f

                        /** Дни недели */
                        val weekStatuses = viewModel.buildWeekStatuses(habit, logs)
                        dayAdapter.submitList(weekStatuses)

                        /** Сводка */
                        val doneCount = weekStatuses.count {
                            it.status == DayCompletionStatus.DONE ||
                                    it.status == DayCompletionStatus.PARTIAL
                        }
                        val totalDays = weekStatuses.count {
                            it.status != DayCompletionStatus.NOT_CREATED
                        }

                        binding.tvWeekSummary.text = resources.getQuantityString(
                            R.plurals.habit_detail_week_summary_format,
                            totalDays,
                            doneCount,
                            totalDays
                        )

                        if (habit.type == HabitType.WEEKLY)
                            binding.tvWeekSummary.visibility = GONE
                    }
                }.collect { }
            }
        }
    }

    private fun showHabitMenu() {
        val popup = PopupMenu(requireContext(), binding.btnMenu)
        popup.menu.add(0, 1, 0, getString(R.string.habit_detail_menu_rename))
        //  popup.menu.add(0, 2, 1, getString(R.string.habit_detail_menu_delete))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    val habit = viewModel.habit.value
                    if (habit != null) showRenameDialog(habit)
                    true
                }

                /*  2 -> {
                      //todo  В будущие релизы
                      Toast.makeText(requireContext(), "Скоро будет", Toast.LENGTH_SHORT).show()
                      true
                  }*/

                else -> false
            }
        }

        popup.show()
    }

    private fun showRenameDialog(habit: HabitEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = viewModel.getCategories()

            val dialog = RenameHabitDialogFragment().apply {
                this.habitName = habit.name
                this.currentCategoryId = habit.categoryId
                this.categories = categories

                onHabitUpdated = { newName, newCategoryId ->
                    viewModel.updateHabit(
                        habit.copy(
                            name = newName,
                            categoryId = newCategoryId
                        )
                    )
                }
            }
            dialog.show(childFragmentManager, "rename_habit")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}