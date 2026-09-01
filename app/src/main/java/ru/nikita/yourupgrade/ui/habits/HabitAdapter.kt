package ru.nikita.yourupgrade.ui.habits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.data.model.HabitType
import ru.nikita.yourupgrade.databinding.ItemHabitBinding

class HabitAdapter(
    private val onItemClick: (HabitUiItem) -> Unit,
    private val onDeleteClick: (HabitUiItem) -> Unit
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
            binding.tvTarget.text = item.targetLabel

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    private class HabitDiffCallback : DiffUtil.ItemCallback<HabitUiItem>() {
        override fun areItemsTheSame(oldItem: HabitUiItem, newItem: HabitUiItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HabitUiItem, newItem: HabitUiItem): Boolean =
            oldItem == newItem
    }
}