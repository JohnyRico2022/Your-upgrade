package ru.nikita.yourupgrade.ui.habits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.databinding.ItemDayStatusBinding

class DayStatusAdapter : ListAdapter<DayStatusUiItem, DayStatusAdapter.DayViewHolder>(DayDiffCallback()) {

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
            binding.tvStatus.text = item.statusLabel

            // Цвет статуса
            val statusColor = when {
                item.isFullyDone -> 0xFF4CAF50.toInt()   // зелёный
                item.isDone || item.completedValue > 0f -> 0xFFFF9800.toInt()  // оранжевый (частично)
                else -> 0xFF9E9E9E.toInt()  // серый
            }
            binding.tvStatus.setTextColor(statusColor)

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

        override fun areContentsTheSame(oldItem: DayStatusUiItem, newItem: DayStatusUiItem): Boolean =
            oldItem == newItem
    }
}