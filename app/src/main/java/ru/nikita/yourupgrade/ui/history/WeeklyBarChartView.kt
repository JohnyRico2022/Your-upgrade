package ru.nikita.yourupgrade.ui.history

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var dayPoints: List<Float> = emptyList()
    private var selectedDayIndex: Int = -1
    private var onDayClickListener: ((Int) -> Unit)? = null

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val negativeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        strokeWidth = 2f
    }

    private val barRect = RectF()
    private val barWidthRatio = 0.6f  // Ширина столбика относительно ячейки

    // Цвета
    private val positiveColor = Color.parseColor("#4CAF50")  // Зелёный
    private val negativeColor = Color.parseColor("#E53935")  // Красный
    private val selectedColor = Color.parseColor("#FFC107")  // Жёлтый (выбранный)

    init {
        isClickable = true
    }

    fun setData(points: List<Float>, selectedIndex: Int = -1) {
        dayPoints = points
        selectedDayIndex = selectedIndex
        invalidate()
    }

    fun setOnDayClickListener(listener: (Int) -> Unit) {
        onDayClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dayPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val cellWidth = width / dayPoints.size

        // Максимальное значение для масштабирования
        val maxAbsValue = dayPoints.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f

        // Базовая линия (середина высоты)
        val baseline = height * 0.7f  // 70% от высоты — базовая линия

        // Рисуем базовую линию
        canvas.drawLine(0f, baseline, width, baseline, linePaint)

        // Рисуем столбики
        dayPoints.forEachIndexed { index, points ->
            val centerX = cellWidth * index + cellWidth / 2
            val barWidth = cellWidth * barWidthRatio
            val left = centerX - barWidth / 2
            val right = centerX + barWidth / 2

            // Высота столбика пропорциональна баллам
            val barHeight = (kotlin.math.abs(points) / maxAbsValue) * (height * 0.6f)

            // Выбираем цвет
            val paint = when {
                index == selectedDayIndex -> {
                    selectedBarPaint.apply { color = selectedColor }
                }
                points >= 0 -> {
                    barPaint.apply { color = positiveColor }
                }
                else -> {
                    negativeBarPaint.apply { color = negativeColor }
                }
            }

            if (points >= 0) {
                // Положительный столбик — вверх от базовой линии
                barRect.set(left, baseline - barHeight, right, baseline)
            } else {
                // Отрицательный столбик — вниз от базовой линии
                barRect.set(left, baseline, right, baseline + barHeight)
            }

            canvas.drawRoundRect(barRect, 8f, 8f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cellWidth = width.toFloat() / dayPoints.size
            val clickedIndex = (event.x / cellWidth).toInt()

            if (clickedIndex in dayPoints.indices) {
                Log.d("MyLog", "Клик по дню: $clickedIndex, баллы: ${dayPoints[clickedIndex]}")
                onDayClickListener?.invoke(clickedIndex)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}