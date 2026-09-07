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

                    // 🆕 Зелёный фон для выполненных
                    val cardView = binding.root
                    if (item.isFullyDone) {
                        cardView.setCardBackgroundColor(0xFFE8F5E9.toInt())  // светло-зелёный
                        binding.tvHabitName.alpha = 0.6f
                    }

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

                    // 🆕 Цвет в зависимости от процента
                    val percent = if (item.targetValue > 0) {
                        item.completedValue / item.targetValue
                    } else 0f

                    val progressColor = when {
                        percent == 0f -> 0xFF757575.toInt()        // серый (не начинал)
                        percent <= 0.5f -> 0xFFE53935.toInt()      // красный (0-50%)
                        percent < 1.0f -> 0xFFFFB300.toInt()       // жёлтый (51-99%)
                        else -> 0xFF4CAF50.toInt()                  // зелёный (100%+)
                    }
                    binding.tvProgress.setTextColor(progressColor)

                    // 🆕 Зелёный фон карточки при 100%
                    val cardView = binding.root as com.google.android.material.card.MaterialCardView
                    if (percent >= 1.0f) {
                        cardView.setCardBackgroundColor(0xFFE8F5E9.toInt())
                        binding.tvHabitName.alpha = 0.6f
                    }

                    binding.tvProgress.setOnClickListener {
                        onProgressClick(item)
                    }
                }
            }

            /// Полупрозрачность для выполненных
//            val alpha = if (item.isDoneToday || item.isFullyDone) 0.6f else 1.0f
//            binding.tvHabitName.alpha = alpha
//            binding.tvCategory.alpha = alpha

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