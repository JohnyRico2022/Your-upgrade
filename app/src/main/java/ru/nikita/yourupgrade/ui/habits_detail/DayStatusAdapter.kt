package ru.nikita.yourupgrade.ui.habits_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.databinding.ItemDayStatusBinding
import ru.nikita.yourupgrade.ui.habits.DayCompletionStatus
import ru.nikita.yourupgrade.ui.habits.DayStatusUiItem

class DayStatusAdapter :
    ListAdapter<DayStatusUiItem, DayStatusAdapter.DayViewHolder>(DayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayStatusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DayViewHolder(
        private val binding: ItemDayStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DayStatusUiItem) {
            binding.tvDayName.text = item.dayShortName

            when (item.completionMode) {
                CompletionMode.CHECKABLE -> {
                    when (item.status) {
                        DayCompletionStatus.DONE -> {
                            binding.tvStatus.text = "✓"
                            binding.tvStatus.setTextColor(0xFF4CAF50.toInt())  // Зелёный
                        }

                        DayCompletionStatus.MISSED -> {
                            binding.tvStatus.text = "✗"
                            binding.tvStatus.setTextColor(0xFFE53935.toInt())  // Красный
                        }

                        DayCompletionStatus.NOT_CREATED -> {
                            binding.tvStatus.text = "—"
                            binding.tvStatus.setTextColor(0xFF9E9E9E.toInt())  // Серый
                        }

                        DayCompletionStatus.PARTIAL -> {
                            // Для отмечаемых не должно быть
//                            binding.tvStatus.text = "✓"
//                            binding.tvStatus.setTextColor(0xFF4CAF50.toInt())
                        }
                    }
                }

                CompletionMode.MEASURABLE -> {

                    when (item.status) {
                        DayCompletionStatus.DONE -> {
                            binding.tvStatus.text =
                                "${item.completedValue.toInt()}/${item.targetValue.toInt()}"
                            binding.tvStatus.setTextColor(0xFF4CAF50.toInt())
                        }

                        DayCompletionStatus.MISSED -> {
                            binding.tvStatus.text =
                                "${item.completedValue.toInt()}/${item.targetValue.toInt()}"
                            binding.tvStatus.setTextColor(0xFFE53935.toInt())
                        }

                        DayCompletionStatus.NOT_CREATED -> {
                            binding.tvStatus.text = "—"
                            binding.tvStatus.setTextColor(0xFF9E9E9E.toInt())
                        }

                        // 🆕 Частичное выполнение: жёлтый
                        DayCompletionStatus.PARTIAL -> {
                            binding.tvStatus.text =
                                "${item.completedValue.toInt()}/${item.targetValue.toInt()}"
                            binding.tvStatus.setTextColor(0xFFB8860B.toInt())  // Тёмно-жёлтый текст
                        }
                    }
                }
            }


            // Подсветка сегодняшнего дня
            if (item.isToday) {
                binding.tvDayName.setTextColor(0xFF2196F3.toInt())  // синий
                binding.tvDayName.paint.isFakeBoldText = true
            } else {
                binding.tvDayName.setTextColor(0xFF757575.toInt())
                binding.tvDayName.paint.isFakeBoldText = false
            }
        }
    }

    private class DayDiffCallback : DiffUtil.ItemCallback<DayStatusUiItem>() {
        override fun areItemsTheSame(oldItem: DayStatusUiItem, newItem: DayStatusUiItem): Boolean =
            oldItem.date == newItem.date

        override fun areContentsTheSame(
            oldItem: DayStatusUiItem,
            newItem: DayStatusUiItem
        ): Boolean =
            oldItem == newItem
    }
}