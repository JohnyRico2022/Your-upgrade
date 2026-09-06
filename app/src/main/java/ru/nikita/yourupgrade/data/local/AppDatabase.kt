package ru.nikita.yourupgrade.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.data.local.dao.CategoryDao
import ru.nikita.yourupgrade.data.local.dao.HabitDao
import ru.nikita.yourupgrade.data.local.dao.HabitLogDao
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.data.local.entity.HabitLogEntry

@Database(
    entities = [
        HabitEntity::class,
        SkillCategoryEntity::class,
        HabitLogEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitLogDao(): HabitLogDao  // 🆕

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
                    .fallbackToDestructiveMigration()  // для разработки: сброс при изменении версии
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
                            SkillCategoryEntity(name = "Дисциплина", weight = 1.0f, iconEmoji = "🎯"),
                            SkillCategoryEntity(name = "Здоровье", weight = 1.0f, iconEmoji = "\uD83D\uDC8A")
                        )
                    )
                }
            }
        }
    }
}