package ru.nikita.yourupgrade.util

object UnitsHelper {

    // Форматирование значения с единицей
    fun formatValueWithUnit(value: Float, unit: String): String {
        val intValue = value.toInt()
        return if (unit.isNotBlank()) {
            "$intValue $unit"
        } else {
            "$intValue"
        }
    }

    // Форматирование прогресса: "15/20 минут"
    fun formatProgress(completed: Float, target: Float, unit: String): String {
        return if (unit.isNotBlank()) {
            "${completed.toInt()}/${target.toInt()} $unit"
        } else {
            "${completed.toInt()}/${target.toInt()}"
        }
    }
}