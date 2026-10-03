package ru.nikita.yourupgrade.ui.habits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.databinding.DialogAddHabitBinding
import ru.nikita.yourupgrade.ui.user.CategoryAdapter
import ru.nikita.yourupgrade.util.UnitsHelper

class AddHabitDialogFragment : DialogFragment() {

    private var _binding: DialogAddHabitBinding? = null
    private val binding get() = _binding!!
    private var selectedCategoryId: Long = -1L
    private lateinit var categoryAdapter: CategorySelectorAdapter
    var onHabitCreated: ((String, Long, HabitType, CompletionMode, Float, String) -> Unit)? = null
    private var categories: List<SkillCategoryEntity> = emptyList()

    fun setCategories(categories: List<SkillCategoryEntity>) {
        this.categories = categories
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

          dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupCategories()
        setupModeRadioGroup()
        setupListeners()
    }

    private fun setupCategories() {
        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "Нет доступных категорий", Toast.LENGTH_SHORT).show()
            return
        }

        // Создаём адаптер для спиннера
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { "${it.iconEmoji} ${it.name}" }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Обработчик выбора
        binding.spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedCategoryId = categories[position].id
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Ничего не делаем
            }
        }

        // Выбираем первую категорию по умолчанию
        selectedCategoryId = categories[0].id

    }

    private fun setupModeRadioGroup() {
        binding.radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            val isMeasurable = checkedId == R.id.radioMeasurable
            binding.measurableFields.visibility = if (isMeasurable) View.VISIBLE else View.GONE
        }
    }


    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createHabit()
        }
    }

    private fun createHabit() {
        val name = binding.etHabitName.text.toString().trim()
        if (name.isBlank()) {
            binding.etHabitName.error = "Введите название"
            return
        }

        if (selectedCategoryId == -1L) {
            Toast.makeText(requireContext(), "Выберите категорию", Toast.LENGTH_SHORT).show()
            return
        }

        // 🆕 Читаем RadioButton
        val type = if (binding.radioDaily.isChecked) HabitType.DAILY else HabitType.WEEKLY
        val mode = if (binding.radioCheckable.isChecked) CompletionMode.CHECKABLE else CompletionMode.MEASURABLE

        var targetValue = 1f
        var unit = ""

        // 🆕 Для измеряемых привычек — цель и единица
        if (mode == CompletionMode.MEASURABLE) {
            val targetText = binding.etTargetValue.text.toString()
            if (targetText.isBlank()) {
                binding.etTargetValue.error = "Введите цель"
                return
            }

            targetValue = targetText.toFloatOrNull() ?: 0f
            if (targetValue <= 0) {
                binding.etTargetValue.error = "Цель должна быть больше 0"
                return
            }

            // 🆕 Единица измерения — свободный ввод
            unit = binding.etUnit.text.toString().trim()
        }

        onHabitCreated?.invoke(name, selectedCategoryId, type, mode, targetValue, unit)
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}