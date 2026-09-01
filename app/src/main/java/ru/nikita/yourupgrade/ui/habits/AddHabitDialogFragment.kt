package ru.nikita.yourupgrade.ui.habits

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.databinding.DialogAddHabitBinding

class AddHabitDialogFragment : DialogFragment() {

    private var _binding: DialogAddHabitBinding? = null
    private val binding get() = _binding!!

    var onHabitCreated: ((String, Long, HabitType, CompletionMode, Float) -> Unit)? = null

    private var categories: List<SkillCategoryEntity> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddHabitBinding.inflate(layoutInflater)

        loadCategories()
        setupCompletionModeToggle()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Добавить") { _, _ -> submitHabit() }
            .setNegativeButton("Отмена", null)
            .create()
    }

    private fun setupCompletionModeToggle() {
        binding.rgCompletionMode.setOnCheckedChangeListener { _, checkedId ->
            val isMeasurable = checkedId == binding.rbMeasurable.id
            binding.etTarget.visibility = if (isMeasurable) View.VISIBLE else View.GONE
        }
    }

    private fun loadCategories() {
        val db = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            categories = db.categoryDao().getAllCategories().first()
            val names = categories.map { "${it.iconEmoji} ${it.name}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                names
            )
            binding.spinnerCategory.adapter = adapter
        }
    }

    private fun submitHabit() {
        val name = binding.etHabitName.text.toString().trim()
        if (name.isEmpty()) return

        val selectedCategoryIndex = binding.spinnerCategory.selectedItemPosition
        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categories.size) return

        val categoryId = categories[selectedCategoryIndex].id

        val type = if (binding.rbDaily.isChecked) HabitType.DAILY else HabitType.WEEKLY

        val completionMode = if (binding.rbMeasurable.isChecked) {
            CompletionMode.MEASURABLE
        } else {
            CompletionMode.CHECKABLE
        }

        val target = if (completionMode == CompletionMode.MEASURABLE) {
            binding.etTarget.text.toString().toFloatOrNull() ?: 1f
        } else {
            1f
        }

        onHabitCreated?.invoke(name, categoryId, type, completionMode, target)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}