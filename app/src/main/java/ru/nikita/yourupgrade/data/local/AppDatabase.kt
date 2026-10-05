package ru.nikita.yourupgrade.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.dao.AchievementDao
import ru.nikita.yourupgrade.data.local.dao.AppUsageDao
import ru.nikita.yourupgrade.data.local.dao.CategoryDao
import ru.nikita.yourupgrade.data.local.dao.HabitDao
import ru.nikita.yourupgrade.data.local.dao.HabitLogDao
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.local.entity.AppUsageEntity
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity

@Database(
    entities = [
        HabitEntity::class,
        SkillCategoryEntity::class,
        HabitLogEntry::class,
        AchievementEntity::class,
        AppUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun achievementDao(): AchievementDao
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yourself_upgrade_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {

                    val categoryDao = database.categoryDao()
                    val categories = listOf(
                        SkillCategoryEntity(
                            name = "Сила",
                            weight = 1.0f,
                            iconEmoji = "💪",
                            isUserCreated = false
                        ),
                        SkillCategoryEntity(
                            name = "Интелект",
                            weight = 1.0f,
                            iconEmoji = "🧠",
                            isUserCreated = false
                        ),
                        SkillCategoryEntity(
                            name = "Дисциплина",
                            weight = 1.0f,
                            iconEmoji = "🎯", isUserCreated = false
                        ),
                        SkillCategoryEntity(
                            name = "Здоровье",
                            weight = 1.0f,
                            iconEmoji = "\uD83D\uDC8A", isUserCreated = false
                        )
                    )

                    categories.forEach { categoryDao.insertCategory(it) }
                }
            }
        }
    }
}