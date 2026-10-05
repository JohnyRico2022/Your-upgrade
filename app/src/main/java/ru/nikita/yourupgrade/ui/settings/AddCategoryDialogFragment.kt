package ru.nikita.yourupgrade.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import ru.nikita.yourupgrade.databinding.DialogAddCategoryBinding

class AddCategoryDialogFragment : DialogFragment() {

    private var _binding: DialogAddCategoryBinding? = null
    private val binding get() = _binding!!

    private var selectedEmoji: String? = null
    private lateinit var emojiAdapter: EmojiSelectorAdapter

    var onCategoryCreated: ((name: String, emoji: String) -> Unit)? = null

    // 🆕 10 эмодзи на выбор
    companion object {
        val AVAILABLE_EMOJIS = listOf(
            "💰", // Финансы
            "🎨", // Творчество
            "👨‍👩‍👧", // Семья/Отношения
            "😴", // Сон/Отдых
            "🚗", // Путешествия
            "🧘", // Медитация
            "📚", // Чтение
            "💼", // Карьера
            "🎮", // Хобби
            "⏰"  // Продуктивность
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupEmojis()
        setupListeners()
    }

    private fun setupEmojis() {
        emojiAdapter = EmojiSelectorAdapter { emoji ->
            selectedEmoji = emoji
            emojiAdapter.setSelectedEmoji(emoji)
        }

        binding.rvEmojis.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvEmojis.adapter = emojiAdapter
        emojiAdapter.submitList(AVAILABLE_EMOJIS)
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createCategory()
        }
    }

    private fun createCategory() {
        val name = binding.etCategoryName.text.toString().trim()
        if (name.isBlank()) {
            binding.etCategoryName.error = "Введите название"
            return
        }

        if (selectedEmoji == null) {
            Toast.makeText(requireContext(), "Выберите иконку", Toast.LENGTH_SHORT).show()
            return
        }

        onCategoryCreated?.invoke(name, selectedEmoji!!)
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