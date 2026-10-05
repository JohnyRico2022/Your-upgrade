package ru.nikita.yourupgrade.ui.habits_detail

import android.app.AlertDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.R
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
        setupSwipeGestures()
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
                                // Свайп влево → следующая неделя (если можно)
                                if (viewModel.canGoNext()) {
                                    viewModel.nextWeek()
                                }
                            }
                            return true
                        }
                    }
                    return false
                }
            })

        binding.cardWeek.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }


    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.habit, viewModel.weekLogs) { habit, logs ->
                    if (habit != null) {
                        /** Заголовок */
                        binding.tvHabitName.text = habit.name

                        binding.btnMenu.setOnClickListener {
                            showHabitMenu()
                        }

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
                    }
                }.collect { }
            }
        }
    }

    private fun showHabitMenu() {
        val popup = PopupMenu(requireContext(), binding.btnMenu)
        popup.menu.add(0, 1, 0, getString(R.string.habit_detail_menu_rename))
        popup.menu.add(0, 2, 1, getString(R.string.habit_detail_menu_delete))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    showRenameDialog()
                    true
                }

                2 -> {
                    //todo  В будущие релизы
                    Toast.makeText(requireContext(), "Скоро будет", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    //todo переделать диалог
    private fun showRenameDialog() {
        val currentName = binding.tvHabitName.text.toString()

        val editText = EditText(requireContext()).apply {
            setText(currentName)
            hint = "Новое название"
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Переименовать привычку")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renameHabit(newName)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}