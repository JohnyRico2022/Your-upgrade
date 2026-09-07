package ru.nikita.yourupgrade

import android.app.Application
import android.util.Log
import ru.nikita.yourupgrade.alarm.AlarmScheduler
import ru.nikita.yourupgrade.alarm.NotificationHelper
import ru.nikita.yourupgrade.data.local.AppDatabase

class App : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        try {
            database

            NotificationHelper.createNotificationChannel(this)

            AlarmScheduler.cancelAll(this)
            AlarmScheduler.scheduleAll(this)
        } catch (e: Exception) {
            Log.e("MyLog", "❌ App.onCreate CRASHED", e)
        }
    }
}