package ru.nikita.yourupgrade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey
    val date: String  // "yyyy-MM-dd"
)