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
import ru.nikita.yourupgrade.data.local.entity.HabitEntity
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity

@Database(
    entities = [HabitEntity::class, SkillCategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao

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
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Предзаполнение категорий при первом запуске
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val categoryDao = database.categoryDao()
                    categoryDao.insertCategories(
                        listOf(
                            SkillCategoryEntity(name = "Сила", weight = 1.0f, iconEmoji = "💪"),
                            SkillCategoryEntity(
                                name = "Выносливость",
                                weight = 1.0f,
                                iconEmoji = "🏃"
                            ),
                            SkillCategoryEntity(
                                name = "Ментальный",
                                weight = 1.0f,
                                iconEmoji = "🧠"
                            ),
                            SkillCategoryEntity(
                                name = "Дисциплина",
                                weight = 1.0f,
                                iconEmoji = "🎯"
                            ),
                            SkillCategoryEntity(name = "Творческий", weight = 1.0f, iconEmoji = "🎨")
                        )
                    )
                }
            }
        }
    }
}