package ru.nikita.yourupgrade.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.databinding.BottomMenuBinding

class BottomMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding: BottomMenuBinding =
        BottomMenuBinding.inflate(LayoutInflater.from(context), this, true)

    private val tabs: List<BottomMenuItem> by lazy {
        listOf(
            binding.tabHabits,
            binding.tabHistory,
            binding.tabRewards,
            binding.tabSettings
        )
    }

    var onTabSelected: ((Int) -> Unit)? = null
    private var selectedPosition = 0

    init {
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                selectTab(index)
            }
        }
    }

    fun selectTab(position: Int) {
        if (position == selectedPosition) return

        tabs[selectedPosition].isActive = false
        tabs[position].isActive = true
        selectedPosition = position
        onTabSelected?.invoke(position)
    }

    /**
     * Вызывается из Fragment/Activity при программной навигации
     */
    fun setActiveByFragment(fragmentId: Int) {
        val position = when (fragmentId) {
            R.id.habitsFragment -> 0
            R.id.historyFragment -> 1
            R.id.rewardsFragment -> 2
            R.id.settingsFragment -> 3
            else -> -1
        }
        if (position >= 0 && position != selectedPosition) {
            tabs[selectedPosition].isActive = false
            tabs[position].isActive = true
            selectedPosition = position
        }
    }
}