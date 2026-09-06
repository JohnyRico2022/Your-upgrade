package ru.nikita.yourupgrade.ui.habits

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.databinding.ItemHabitBinding

class HabitAdapter(
    private val onItemClick: (HabitUiItem) -> Unit,
    private val onDeleteClick: (HabitUiItem) -> Unit,
    private val onMarkDone: (HabitUiItem) -> Unit,      // 🆕 только отметка
    private val onUnmarkDone: (HabitUiItem) -> Unit,    // 🆕 снятие отметки
    private val onProgressClick: (HabitUiItem) -> Unit  // 🆕 для MEASURABLE
) : ListAdapter<HabitUiItem, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(
        private val binding: ItemHabitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HabitUiItem) {
            binding.tvHabitName.text = item.name
            binding.tvCategory.text = "${item.categoryEmoji} ${item.categoryName}"
            binding.tvType.text = if (item.type == HabitType.DAILY) "Ежедневно" else "Еженедельно"

            binding.root.setOnClickListener { onItemClick(item) }

            // 🆕 Разная логика для разных типов
            when (item.completionMode) {
                CompletionMode.CHECKABLE -> {
                    binding.cbDone.visibility = View.VISIBLE
                    binding.tvProgress.visibility = View.GONE

                    binding.cbDone.isChecked = item.isDoneToday
                    binding.cbDone.setOnClickListener {
                        if (item.isDoneToday) {
                            // 🆕 Снятие отметки — через подтверждение
                            showUnmarkConfirmation(item)
                        } else {
                            // Отметка — сразу, без диалога
                            onMarkDone(item)
                        }

                    }
                }

                CompletionMode.MEASURABLE -> {
                    binding.cbDone.visibility = View.GONE
                    binding.tvProgress.visibility = View.VISIBLE

                    binding.tvProgress.text = item.progressLabel

                    // Цвет прогресса: зелёный если выполнено, обычный если нет
                    val color = if (item.isFullyDone) {
                        0xFF4CAF50.toInt()  // зелёный
                    } else {
                        0xFF757575.toInt()  // серый
                    }
                    binding.tvProgress.setTextColor(color)

                    binding.tvProgress.setOnClickListener {
                        onProgressClick(item)
                    }
                }
            }

            /// Полупрозрачность для выполненных
            val alpha = if (item.isDoneToday || item.isFullyDone) 0.6f else 1.0f
            binding.tvHabitName.alpha = alpha
            binding.tvCategory.alpha = alpha

            binding.root.setOnClickListener { onItemClick(item) }
        }

        // 🆕 Диалог подтверждения снятия отметки
        private fun showUnmarkConfirmation(item: HabitUiItem) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Отменить отметку?")
                .setMessage("Вы уверены, что хотите снять отметку выполнения для «${item.name}»?")
                .setPositiveButton("Да, отменить") { _, _ ->
                    onUnmarkDone(item)
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }

    private class HabitDiffCallback : DiffUtil.ItemCallback<HabitUiItem>() {
        override fun areItemsTheSame(oldItem: HabitUiItem, newItem: HabitUiItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HabitUiItem, newItem: HabitUiItem): Boolean =
            oldItem == newItem
    }
}