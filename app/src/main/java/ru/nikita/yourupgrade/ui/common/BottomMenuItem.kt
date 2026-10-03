package ru.nikita.yourupgrade.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.databinding.BottomMenuItemBinding

class BottomMenuItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = BottomMenuItemBinding.inflate(LayoutInflater.from(context), this, true)

    var isActive: Boolean = false
        set(value) {
            field = value
            updateAppearance()
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.BottomMenuItem).apply {
            val icon = getDrawable(R.styleable.BottomMenuItem_icon)
            val hintText = getString(R.styleable.BottomMenuItem_hintText)

            if (icon != null) binding.icon.setImageDrawable(icon)
            if (hintText != null) binding.hint.text = hintText

            recycle()
        }

        setBackgroundResource(android.R.color.transparent)
        isClickable = true
        isFocusable = true
    }

    private fun updateAppearance() {
        val tintColor = if (isActive) {
            context.getColor(R.color.white)
        } else {
            context.getColor(R.color.black)
        }

        binding.icon.setColorFilter(tintColor)
        binding.hint.setTextColor(tintColor)

        // Анимация масштабирования при активации
        animate()
            .scaleX(if (isActive) 1.2f else 1.0f)
            .scaleY(if (isActive) 1.2f else 1.0f)
            .setDuration(150)
            .start()
    }
}