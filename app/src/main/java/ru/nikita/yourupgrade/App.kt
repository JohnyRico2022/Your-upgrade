package ru.nikita.yourupgrade

import android.app.Application
import ru.nikita.yourupgrade.data.local.AppDatabase

class App : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Инициализация БД при старте (триггерит onCreate callback с категориями)
        database
    }
}