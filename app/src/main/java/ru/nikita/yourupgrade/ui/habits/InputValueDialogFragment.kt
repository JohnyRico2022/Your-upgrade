package ru.nikita.yourupgrade.ui.habits

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import ru.nikita.yourupgrade.databinding.DialogInputValueBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

class InputValueDialogFragment : DialogFragment() {

    private var _binding: DialogInputValueBinding? = null
    private val binding get() = _binding!!

    var habitName: String = ""
    var targetValue: Float = 1f
    var currentValue: Float = 0f
    var onValueEntered: ((Float) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogInputValueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.tvDialogTitle.text = habitName
        binding.tvTargetInfo.text = "Цель: ${targetValue.toInt()}"

        binding.etValue.setSelectAllOnFocus(true)

        // Показываем текущее значение если есть
        if (currentValue > 0) {
            binding.tvCurrentValue.visibility = View.VISIBLE
            binding.tvCurrentValue.text = "Сейчас: ${currentValue.toInt()}"
            binding.etValue.setText(currentValue.toInt().toString())
        }

        binding.etValue.postDelayed({
            binding.etValue.requestFocus()
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etValue, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val text = binding.etValue.text.toString()

            if (text.isBlank()) {
                binding.etValue.error = "Введите значение"
                return@setOnClickListener
            }

            val value = text.toFloatOrNull()
            if (value == null || value < 0) {
                binding.etValue.error = "Некорректное значение"
                return@setOnClickListener
            }

            onValueEntered?.invoke(value)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Ширина диалога 90% от экрана
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