package ru.nikita.yourupgrade.ui.rewards

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.nikita.yourupgrade.databinding.ItemAchievementCategoryBinding

class AchievementAdapter : ListAdapter<AchievementCategoryUi, AchievementAdapter.AchievementViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("MyLog", "onBind: pos=$position, type=${item.type.name}, title=${item.title}")
        holder.bind(getItem(position))
    }

    inner class AchievementViewHolder(
        private val binding: ItemAchievementCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AchievementCategoryUi) {
            binding.tvCategoryIcon.text = item.iconEmoji
            binding.tvCategoryTitle.text = item.title
            binding.tvCategoryDescription.text = item.description

            // 🆕 Заполняем уровни
            binding.levelsContainer.removeAllViews()

            item.levels.forEach { level ->
                val levelView = createLevelView(level)
                binding.levelsContainer.addView(levelView)
            }
        }

        // 🆕 Создаём View для одного уровня
        private fun createLevelView(level: AchievementLevelUi): View {
            val context = binding.root.context

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dpToPx(4)
                    marginEnd = dpToPx(4)
                }
                gravity = android.view.Gravity.CENTER

                // 🆕 Фон в зависимости от статуса
                if (level.isEarned) {
                    // Яркий фон для заработанных
                    setBackgroundColor(0xFFFFF8E1.toInt())  // Светло-золотой
                } else {
                    // Серый фон для незаработанных
                    setBackgroundColor(0xFFF5F5F5.toInt())  // Светло-серый
                }

                setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))

                // Скругление углов через background
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(
                        if (level.isEarned) 0xFFFFF8E1.toInt()
                        else 0xFFF5F5F5.toInt()
                    )
                }
            }

            // Иконка уровня
            val iconView = TextView(context).apply {
                text = if (level.isEarned) "🏅" else "🔒"
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                // 🆕 Серый фильтр для незаработанных
                if (!level.isEarned) {
                    alpha = 0.4f
                }
            }

            // Уровень
            val levelTextView = TextView(context).apply {
                text = "Ур. ${level.level}"
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(
                    if (level.isEarned) 0xFFD5A72D.toInt()
                    else 0xFF9E9E9E.toInt()
                )
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            // Прогресс или дата
            val progressView = TextView(context).apply {
                text = when {
                    level.isEarned -> "✓"
                    else -> "${level.currentProgress}/${level.threshold}"
                }
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setTextColor(
                    when {
                        level.isEarned -> 0xFF4CAF50.toInt()
                        else -> 0xFF9E9E9E.toInt()
                    }
                )
                setPadding(0, dpToPx(4), 0, 0)
            }

            // Очки
            val pointsView = TextView(context).apply {
                text = "+${level.points}"
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setTextColor(
                    if (level.isEarned) 0xFF4CAF50.toInt()
                    else 0xFFBDBDBD.toInt()
                )
                setPadding(0, dpToPx(2), 0, 0)
            }

            container.addView(iconView)
            container.addView(levelTextView)
            container.addView(progressView)
            container.addView(pointsView)

            return container
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * binding.root.context.resources.displayMetrics.density).toInt()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AchievementCategoryUi>() {
        override fun areItemsTheSame(old: AchievementCategoryUi, new: AchievementCategoryUi) =
            old.type == new.type

        override fun areContentsTheSame(old: AchievementCategoryUi, new: AchievementCategoryUi) =
            old == new
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d("AchievementAdapter", "getItemCount: $count")
        return count
    }

}