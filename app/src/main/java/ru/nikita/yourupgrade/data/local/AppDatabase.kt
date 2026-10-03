package ru.nikita.yourupgrade.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.dao.AchievementDao
import ru.nikita.yourupgrade.data.local.dao.AppUsageDao
import ru.nikita.yourupgrade.data.local.dao.CategoryDao
import ru.nikita.yourupgrade.data.local.dao.HabitDao
import ru.nikita.yourupgrade.data.local.dao.HabitLogDao
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.local.entity.AppUsageEntity

@Database(
    entities = [
        HabitEntity::class,
        SkillCategoryEntity::class,
        HabitLogEntry::class,
        AchievementEntity::class,
        AppUsageEntity::class
    ],
    version = 4,
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

        // Миграция с версии 2 на 3: добавляем новые таблицы
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `habitId` INTEGER,
                        `earnedDate` TEXT NOT NULL,
                        `points` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `app_usage` (
                        `date` TEXT NOT NULL,
                        PRIMARY KEY(`date`)
                    )"""
                )
            }
        }

        // Миграция 3 → 4: единицы измерения + цель в логах
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Добавляем единицу измерения в привычки
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN unit TEXT NOT NULL DEFAULT ''"
                )

                // 2. Добавляем цель в логи (для неизменяемой истории)
                db.execSQL(
                    "ALTER TABLE habit_logs ADD COLUMN targetValue REAL NOT NULL DEFAULT 0"
                )

                // 3. Заполняем старые логи текущей целью привычки
                db.execSQL(
                    """UPDATE habit_logs 
                       SET targetValue = (
                           SELECT habits.targetValue 
                           FROM habits 
                           WHERE habits.id = habit_logs.habitId
                       )
                       WHERE targetValue = 0"""
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yourself_upgrade_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)  //
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
                    database.categoryDao().insertCategories(
                        listOf(
                            SkillCategoryEntity(name = "Сила", weight = 1.0f, iconEmoji = "💪"),
                            SkillCategoryEntity(name = "Интелект", weight = 1.0f, iconEmoji = "🧠"),
                            SkillCategoryEntity(
                                name = "Дисциплина",
                                weight = 1.0f,
                                iconEmoji = "🎯"
                            ),
                            SkillCategoryEntity(
                                name = "Здоровье",
                                weight = 1.0f,
                                iconEmoji = "\uD83D\uDC8A"
                            )
                        )
                    )
                }
            }
        }
    }
}