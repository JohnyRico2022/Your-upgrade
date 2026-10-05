package ru.nikita.yourupgrade.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.databinding.ItemEmojiSelectorBinding

class EmojiSelectorAdapter(
    private val onEmojiSelected: (String) -> Unit
) : ListAdapter<String, EmojiSelectorAdapter.EmojiViewHolder>(DiffCallback()) {

    private var selectedEmoji: String? = null

    fun setSelectedEmoji(emoji: String) {
        val old = selectedEmoji
        selectedEmoji = emoji

        currentList.forEachIndexed { index, item ->
            if (item == old || item == emoji) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemEmojiSelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EmojiViewHolder(
        private val binding: ItemEmojiSelectorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(emoji: String) {
            binding.tvEmoji.text = emoji

            val isSelected = emoji == selectedEmoji
            val context = binding.root.context

            if (isSelected) {
                binding.cardRoot.setCardBackgroundColor(0xFFFFF8E1.toInt())
                binding.cardRoot.strokeColor = ContextCompat.getColor(context, R.color.main_gold)
                binding.cardRoot.strokeWidth = dpToPx(2)
            } else {
                binding.cardRoot.setCardBackgroundColor(0xFFF5F5F5.toInt())
                binding.cardRoot.strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
                binding.cardRoot.strokeWidth = 0
            }

            binding.cardRoot.setOnClickListener {
                onEmojiSelected(emoji)
            }
        }

        //todo вынести в Utils
        private fun dpToPx(dp: Int): Int {
            return (dp * binding.root.context.resources.displayMetrics.density).toInt()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(old: String, new: String) = old == new
        override fun areContentsTheSame(old: String, new: String) = old == new
    }
}