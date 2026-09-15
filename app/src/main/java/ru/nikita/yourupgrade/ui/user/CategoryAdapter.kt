package ru.nikita.yourupgrade.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.databinding.ItemCategoryBinding
import java.util.Locale

class CategoryAdapter :
    ListAdapter<CategoryProgress, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    // 🆕 Храним состояние раскрытия по ID категории
    private val expandedCategories = mutableSetOf<Long>()

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

            // 🆕 Состояние раскрытия
            val isExpanded = expandedCategories.contains(item.id)
            binding.habitsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.tvExpandArrow.text = if (isExpanded) "▲" else "▼"

            // 🆕 Заполняем список привычек
            fillHabitsContainer(item, isExpanded)

            // 🆕 Обработчик клика для раскрытия
            binding.categoryHeader.setOnClickListener {
                if (expandedCategories.contains(item.id)) {
                    expandedCategories.remove(item.id)
                } else {
                    expandedCategories.add(item.id)
                }
                notifyItemChanged(adapterPosition)
            }
        }

        // 🆕 Заполняем контейнер привычками
        private fun fillHabitsContainer(
            item: CategoryProgress,
            isExpanded: Boolean
        ) {
            binding.habitsContainer.removeAllViews()

            if (!isExpanded || item.habits.isEmpty()) return

            item.habits.forEach { habit ->
                val habitView = createHabitView(habit)
                binding.habitsContainer.addView(habitView)
            }
        }

        // 🆕 Создаём View для одной привычки
        private fun createHabitView(habit: HabitPointsUi): View {
            val context = binding.root.context

            val container = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(
                    dpToPx(56),  // Отступ слева (под эмодзи)
                    dpToPx(4),
                    dpToPx(16),
                    dpToPx(4)
                )
            }

            // Название привычки
            val nameView = TextView(context).apply {
                text = habit.habitName
                textSize = 14f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Баллы привычки
            val pointsView = TextView(context).apply {
                val pointsText = if (habit.points >= 0) {
                    "+${String.format(Locale.getDefault(), "%.2f", habit.points)}"
                } else {
                    String.format(Locale.getDefault(), "%.2f", habit.points)
                }
                text = pointsText
                textSize = 13f

                val color = if (habit.points >= 0) {
                    0xFF4CAF50.toInt()  // Зелёный
                } else {
                    0xFFE53935.toInt()  // Красный
                }
                setTextColor(color)
            }
            container.addView(nameView)
            container.addView(pointsView)

            return container
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * binding.root.context.resources.displayMetrics.density).toInt()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryProgress>() {
        override fun areItemsTheSame(old: CategoryProgress, new: CategoryProgress) =
            old.id == new.id

        override fun areContentsTheSame(old: CategoryProgress, new: CategoryProgress) = old == new
    }
}