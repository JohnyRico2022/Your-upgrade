package ru.nikita.yourupgrade

import android.app.Application
import android.util.Log
import ru.nikita.yourupgrade.alarm.AlarmScheduler
import ru.nikita.yourupgrade.alarm.NotificationHelper
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.util.PreferencesManager

class App : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        try {
            database

            NotificationHelper.createNotificationChannel(this)

            val prefs = PreferencesManager(this)

            AlarmScheduler.cancelAll(this)
            AlarmScheduler.scheduleAllWithPrefs(this, prefs)
        } catch (e: Exception) {
            Log.e("MyLog", "❌ App.onCreate CRASHED", e)
        }
    }
}