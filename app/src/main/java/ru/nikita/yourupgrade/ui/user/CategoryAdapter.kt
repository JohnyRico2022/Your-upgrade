package ru.nikita.yourupgrade.ui.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.databinding.ItemCategoryBinding
import java.util.Locale

class CategoryAdapter : ListAdapter<CategoryProgress, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryProgress) {
            binding.tvEmoji.text = item.emoji
            binding.tvCategoryName.text = item.name
            binding.tvCategoryPoints.text = String.format(
                Locale.getDefault(),
                "%.2f баллов",
                item.points
            )
            binding.tvCategoryLevel.text = "Ур. ${item.level}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryProgress>() {
        override fun areItemsTheSame(old: CategoryProgress, new: CategoryProgress) = old.id == new.id
        override fun areContentsTheSame(old: CategoryProgress, new: CategoryProgress) = old == new
    }
}