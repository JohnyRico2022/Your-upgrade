package ru.nikita.yourupgrade.ui.habits_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.databinding.DialogRenameHabitBinding

class RenameHabitDialogFragment : DialogFragment() {

    private var _binding: DialogRenameHabitBinding? = null
    private val binding get() = _binding!!

    // Входные данные
    var habitName: String = ""
    var currentCategoryId: Long = -1L
    var categories: List<SkillCategoryEntity> = emptyList()

    var onHabitUpdated: ((newName: String, newCategoryId: Long) -> Unit)? = null

    private var selectedCategoryId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRenameHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Название
        binding.etHabitName.setText(habitName)
        binding.etHabitName.setSelection(habitName.length)  // Курсор в конец

        // Категории
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { "${it.iconEmoji} ${it.name}" }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Выбрать текущую категорию
        val currentIndex = categories.indexOfFirst { it.id == currentCategoryId }
        if (currentIndex >= 0) {
            binding.spinnerCategory.setSelection(currentIndex)
        }

        binding.spinnerCategory.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCategoryId = categories[position].id
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        selectedCategoryId = currentCategoryId
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val newName = binding.etHabitName.text.toString().trim()
            if (newName.isBlank()) {
                binding.etHabitName.error = "Введите название"
                return@setOnClickListener
            }

            onHabitUpdated?.invoke(newName, selectedCategoryId)
            dismiss()
        }
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