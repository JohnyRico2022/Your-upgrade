package ru.nikita.yourupgrade.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MOTIVATION = "ru.nikita.yourupgrade.ACTION_MOTIVATION"
        const val ACTION_REMINDER = "ru.nikita.yourupgrade.ACTION_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MOTIVATION -> {
                NotificationHelper.showMotivation(context)
                // 🆕 Перепланируем на завтра
                AlarmScheduler.scheduleMotivation(context)
            }

            ACTION_REMINDER -> {
                NotificationHelper.showReminder(context)
                // 🆕 Перепланируем на завтра
                AlarmScheduler.scheduleReminder(context)
            }
        }
    }
}