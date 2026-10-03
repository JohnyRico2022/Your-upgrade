package ru.nikita.yourupgrade.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.databinding.ItemDayDetailBinding

class DayDetailAdapter : ListAdapter<HabitDayStatus, DayDetailAdapter.DayDetailViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayDetailViewHolder {
        val binding = ItemDayDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayDetailViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("MyLog", "onBind: pos=$position, habit='${item.habitName}', status=${item.status}")
        holder.bind(item)
    }

    class DayDetailViewHolder(
        private val binding: ItemDayDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HabitDayStatus) {
            binding.tvHabitName.text = "${item.categoryEmoji} ${item.habitName}"

            val (icon, color) = when (item.status) {
                DayHabitStatus.DONE -> "✔️" to 0xFF4CAF50.toInt()
                DayHabitStatus.MISSED -> "❌" to 0xFFE53935.toInt()
                DayHabitStatus.NOT_CREATED -> "➖" to 0xFF9E9E9E.toInt()
            }

            binding.tvStatus.text = icon
            binding.tvStatus.setTextColor(color)

            // Баллы
            val pointsText = when {
                item.status == DayHabitStatus.NOT_CREATED -> ""
                item.earnedPoints >= 0 -> "+${String.format("%.2f", item.earnedPoints)}"
                else -> String.format("%.2f", item.earnedPoints)
            }
            binding.tvPoints.text = pointsText
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<HabitDayStatus>() {
        override fun areItemsTheSame(old: HabitDayStatus, new: HabitDayStatus) =
            old.habitId == new.habitId

        override fun areContentsTheSame(old: HabitDayStatus, new: HabitDayStatus) =
            old == new
    }
}