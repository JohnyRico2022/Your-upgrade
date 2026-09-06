package ru.nikita.yourupgrade.ui.habits

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.nikita.yourupgrade.databinding.DialogInputValueBinding

class InputValueDialogFragment : DialogFragment() {

    private var _binding: DialogInputValueBinding? = null
    private val binding get() = _binding!!

    var habitName: String = ""
    var targetValue: Float = 1f
    var currentValue: Float = 0f

    var onValueEntered: ((Float) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogInputValueBinding.inflate(layoutInflater)

        binding.tvDialogTitle.text = habitName
        binding.tvTargetInfo.text = "Цель: ${targetValue.toLong()}"
        if (currentValue > 0) {
            binding.etValue.setText(currentValue.toString())
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val value = binding.etValue.text.toString().toFloatOrNull() ?: 0f
                onValueEntered?.invoke(value)
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Сбросить") { _, _ ->
                onValueEntered?.invoke(0f)
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}