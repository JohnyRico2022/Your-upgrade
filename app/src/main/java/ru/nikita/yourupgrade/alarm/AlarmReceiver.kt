package ru.nikita.yourupgrade.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MOTIVATION = "ru.nikita.yourupgrade.ACTION_MOTIVATION"
        const val ACTION_REMINDER = "ru.nikita.yourupgrade.ACTION_REMINDER"
        const val ACTION_WEEK_SUMMARY = "ru.nikita.yourupgrade.ACTION_WEEK_SUMMARY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MOTIVATION -> {
                NotificationHelper.showMotivation(context)
                AlarmScheduler.scheduleMotivation(context)
            }

            ACTION_REMINDER -> {
                NotificationHelper.showReminder(context)
                AlarmScheduler.scheduleReminder(context)
            }

            ACTION_WEEK_SUMMARY -> {
                NotificationHelper.showWeekSummary(context)
                AlarmScheduler.scheduleWeekSummary(context)
            }
        }
    }
}