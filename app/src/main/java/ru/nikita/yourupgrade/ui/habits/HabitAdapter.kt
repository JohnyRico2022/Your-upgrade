package ru.nikita.yourupgrade.ui.habits

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.data.model.CompletionMode
import ru.nikita.yourupgrade.databinding.ItemHabitBinding
import ru.nikita.yourupgrade.util.UnitsHelper

class HabitAdapter(
    private val onItemClick: (HabitUiItem) -> Unit,
    private val onMarkDone: (HabitUiItem) -> Unit,
    private val onUnmarkDone: (HabitUiItem) -> Unit,
    private val onProgressClick: (HabitUiItem) -> Unit
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

            binding.root.setOnClickListener { onItemClick(item) }

            when (item.completionMode) {
                // Отмечаемые привычки
                CompletionMode.CHECKABLE -> {
                    binding.imageDone.visibility = View.VISIBLE
                    binding.tvProgress.visibility = View.GONE
                    binding.cardRoot.isSelected = item.isFullyDone

                    binding.cardRoot.isSelected = false
                    binding.cardRoot.isActivated = false

                    if (item.isDoneToday) {
                        binding.cardRoot.isSelected = true
                        binding.imageDone.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_check_circle_24))
                    } else
                        binding.imageDone.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_check_box_outline_blank_24))

                    binding.imageDone.setOnClickListener {
                        if (item.isDoneToday) showUnmarkConfirmation(item)
                        else onMarkDone(item)
                    }
                }

                // Измеряемые привычки
                CompletionMode.MEASURABLE -> {
                    binding.imageDone.visibility = View.GONE
                    binding.tvProgress.visibility = View.VISIBLE
                    //  binding.tvProgress.text = item.progressLabel
                    // 🆕 Форматируем с единицей измерения
                    binding.tvProgress.text = UnitsHelper.formatProgress(
                        completed = item.completedValue,
                        target = item.targetValue,
                        unit = item.unit
                    )

                    // Цвет в зависимости от процента
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

                    binding.cardRoot.isSelected = false
                    binding.cardRoot.isActivated = false

                    when {
                        percent >= 1.0f -> binding.cardRoot.isSelected = true    // Зелёный
                        percent >= 0.5f -> binding.cardRoot.isActivated = true   // Жёлтый
                    }

                    binding.tvProgress.setOnClickListener {
                        onProgressClick(item)
                    }
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

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