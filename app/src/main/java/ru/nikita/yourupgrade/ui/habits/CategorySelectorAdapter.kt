package ru.nikita.yourupgrade.ui.habits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.databinding.ItemCategorySelectorBinding

class CategorySelectorAdapter(
    private val onCategorySelected: (Long) -> Unit
) : ListAdapter<CategorySelectorAdapter.CategoryItem, CategorySelectorAdapter.CategoryViewHolder>(DiffCallback()) {

    data class CategoryItem(
        val id: Long,
        val emoji: String,
        val name: String
    )

    private var selectedId: Long = -1L

    fun setSelectedCategory(id: Long) {
        val oldSelectedId = selectedId
        selectedId = id

        // Обновляем старую и новую выбранную категории
        currentList.forEachIndexed { index, item ->
            if (item.id == oldSelectedId || item.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategorySelectorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem) {
            binding.tvEmoji.text = item.emoji
            binding.tvCategoryName.text = item.name

            val isSelected = item.id == selectedId
            val context = binding.root.context

            if (isSelected) {
                // Выбранная категория
                binding.cardRoot.setCardBackgroundColor(0xFFFFF8E1.toInt())  // Светло-золотой
                binding.cardRoot.strokeColor = ContextCompat.getColor(context, R.color.main_gold)
                binding.cardRoot.strokeWidth = dpToPx(2)
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.main_gold))
            } else {
                // Невыбранная
                binding.cardRoot.setCardBackgroundColor(0xFFF5F5F5.toInt())  // Светло-серый
                binding.cardRoot.strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
                binding.cardRoot.strokeWidth = 0
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            binding.cardRoot.setOnClickListener {
                onCategorySelected(item.id)
            }
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * binding.root.context.resources.displayMetrics.density).toInt()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(old: CategoryItem, new: CategoryItem) = old.id == new.id
        override fun areContentsTheSame(old: CategoryItem, new: CategoryItem) = old == new
    }
}