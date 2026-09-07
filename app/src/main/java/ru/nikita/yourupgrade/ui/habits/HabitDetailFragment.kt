package ru.nikita.yourupgrade.ui.habits

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
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
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.databinding.FragmentHabitDetailBinding

class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitDetailViewModel by viewModels()
    private lateinit var dayAdapter: DayStatusAdapter

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

        // Получаем habitId из аргументов
        val habitId = arguments?.getLong("habitId", -1L) ?: -1L
        if (habitId == -1L) {
            // Если нет ID — возвращаемся
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.setHabitId(habitId)

        setupWeekRecyclerView()
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

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.habit, viewModel.weekLogs) { habit, logs ->
                    if (habit != null) {
                        // Заголовок
                        binding.tvHabitName.text = habit.name

                        binding.btnMenu.setOnClickListener {
                            showHabitMenu()
                        }


                        // Категория (пока без запроса категории, просто тип)
                        val typeLabel = if (habit.type == HabitType.DAILY) "Ежедневно" else "Еженедельно"
                        binding.tvCategoryInfo.text = typeLabel

                        // Цель
                        binding.tvTargetInfo.text = when (habit.completionMode) {
                            CompletionMode.MEASURABLE -> "Цель: ${habit.targetValue.toLong()}"
                            CompletionMode.CHECKABLE -> "Отмечаемая привычка"
                        }

                        // Дни недели
                        val weekStatuses = viewModel.buildWeekStatuses(habit, logs)
                        dayAdapter.submitList(weekStatuses)

                        // Сводка
                        val doneCount = weekStatuses.count { it.isFullyDone }
                        binding.tvWeekSummary.text = "Выполнено: $doneCount из 7 дней"
                    }
                }.collect { }
            }
        }
    }

    // 🆕 Метод показа меню
    private fun showHabitMenu() {
        val popup = PopupMenu(requireContext(), binding.btnMenu)
        popup.menu.add(0, 1, 0, "Переименовать")
        popup.menu.add(0, 2, 1, "Удалить")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    showRenameDialog()
                    true
                }
                2 -> {
                    // Пока не реализуем
                    Toast.makeText(requireContext(), "Скоро будет", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    // 🆕 Диалог переименования
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