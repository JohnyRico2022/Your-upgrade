package ru.nikita.yourupgrade.ui.rewards

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.databinding.FragmentRewardsBinding

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RewardsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.hasEnoughHabits) {
                        binding.cardNotEnoughHabits.visibility = View.GONE
                        binding.rewardsContent.visibility = View.VISIBLE
                        binding.tvTotalAchievementPoints.text = state.totalAchievementPoints.toString()
                        fillAchievements(state.categories)
                    } else {
                        binding.cardNotEnoughHabits.visibility = View.VISIBLE
                        binding.rewardsContent.visibility = View.GONE
                        //todo вынести в класс
                        binding.bannerEmoji.text = "🔒"
                    }
                }
            }
        }
    }

    private fun fillAchievements(categories: List<AchievementCategoryUi>) {
        binding.achievementsContainer.removeAllViews()

        categories.forEach { category ->
            val cardView = createCategoryCard(category)
            binding.achievementsContainer.addView(cardView)
        }
    }

    private fun createCategoryCard(category: AchievementCategoryUi): View {
        val context = requireContext()

        // Внешняя карточка (MaterialCardView)
        val cardView = com.google.android.material.card.MaterialCardView(context).apply {
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = 0xFFE0E0E0.toInt()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
        }

        // Корневой вертикальный LinearLayout
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Заголовок: иконка + название + описание
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconView = TextView(context).apply {
            text = category.iconEmoji
            textSize = 28f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
        }

        val titleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(12)
            }
        }

        val titleView = TextView(context).apply {
            text = category.title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF2D2D2D.toInt())
        }

        val descView = TextView(context).apply {
            text = category.description
            textSize = 13f
            setTextColor(0xFF757575.toInt())
            setPadding(0, dpToPx(2), 0, 0)
        }

        titleContainer.addView(titleView)
        titleContainer.addView(descView)
        headerLayout.addView(iconView)
        headerLayout.addView(titleContainer)

        // Контейнер уровней (горизонтальный)
        val levelsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
        }

        category.levels.forEach { level ->
            val levelView = createLevelView(level)
            levelsLayout.addView(levelView)
        }

        rootLayout.addView(headerLayout)
        rootLayout.addView(levelsLayout)
        cardView.addView(rootLayout)

        return cardView
    }

    private fun createLevelView(level: AchievementLevelUi): View {
        val context = requireContext()

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
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))

            background = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()

                if (level.isEarned) {
                    // ✅ Выполнено: светло-зелёный фон + зелёная рамка
                    setColor(0xFFE8F5E9.toInt())  // Светло-зелёный фон
                    setStroke(dpToPx(2), 0xFF4CAF50.toInt())  // Зелёная рамка 2dp
                } else {
                    // ⏳ Не выполнено: серый фон без рамки
                    setColor(0xFFF5F5F5.toInt())  // Светло-серый фон
                    setStroke(0, 0)  // Без рамки
                }
            }
        }

        // Иконка
        val iconView = TextView(context).apply {
            text = if (level.isEarned) "🏅" else "🔒"
            textSize = 24f
            gravity = Gravity.CENTER
            if (!level.isEarned) alpha = 0.4f
        }

        // Уровень
        val levelTextView = TextView(context).apply {
            text = "Ур. ${level.level}"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(
                if (level.isEarned) 0xFF4CAF50.toInt()  // 🆕 Зелёный для выполненных
                else 0xFF9E9E9E.toInt()
            )
            setTypeface(null, Typeface.BOLD)
        }

        // Прогресс
        val progressView = TextView(context).apply {
            text = when {
                level.isEarned -> "✓"
                else -> "${level.currentProgress}/${level.threshold}"
            }
            textSize = 13f
            gravity = Gravity.CENTER
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
            gravity = Gravity.CENTER
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
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRewards()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}