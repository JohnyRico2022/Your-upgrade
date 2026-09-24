package ru.nikita.yourupgrade.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AlarmScheduler {

    // Request codes
    private const val REQUEST_MOTIVATION = 1001
    private const val REQUEST_REMINDER = 1002

    private const val REQUEST_WEEK_SUMMARY = 1004

    /**
     * Планирует все будильники (вызывается при старте приложения)
     */
    fun scheduleAll(context: Context) {
        scheduleMotivation(context)
        scheduleReminder(context)
        scheduleWeekSummary(context)
    }

    // 🆕 Планируем с учётом настроек
    fun scheduleAllWithPrefs(
        context: Context,
        prefs: ru.nikita.yourupgrade.util.PreferencesManager
    ) {
        if (prefs.isMotivationNotificationEnabled) scheduleMotivation(context)
        else cancelMotivation(context)

        if (prefs.isEveningNotificationEnabled) scheduleReminder(context)
        else cancelReminder(context)

        if (prefs.isWeeklyNotificationEnabled) scheduleWeekSummary(context)
        else cancelWeekSummary(context)
    }


    // 🆕 Методы отмены
    fun cancelMotivation(context: Context) {
        cancelAlarm(context, REQUEST_MOTIVATION, AlarmReceiver.ACTION_MOTIVATION)
    }

    fun cancelReminder(context: Context) {
        cancelAlarm(context, REQUEST_REMINDER, AlarmReceiver.ACTION_REMINDER)
    }

    fun cancelWeekSummary(context: Context) {
        cancelAlarm(context, REQUEST_WEEK_SUMMARY, AlarmReceiver.ACTION_WEEK_SUMMARY)
    }

    private fun cancelAlarm(context: Context, requestCode: Int, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // 15:00 — мотивационное уведомление
    fun scheduleMotivation(context: Context) {
        scheduleAlarm(
            context = context,
            action = AlarmReceiver.ACTION_MOTIVATION,
            requestCode = REQUEST_MOTIVATION,
            hour = 15,
            minute = 0
        )
    }

    // 22:30 — напоминание отметить привычки
    fun scheduleReminder(context: Context) {
        scheduleAlarm(
            context = context,
            action = AlarmReceiver.ACTION_REMINDER,
            requestCode = REQUEST_REMINDER,
            hour = 22,
            minute = 30
        )
    }

    // Универсальный метод планирования будильника
    private fun scheduleAlarm(
        context: Context,
        action: String,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Рассчитываем время следующего срабатывания
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Если время уже прошло сегодня — ставим на завтра
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback: неточный будильник
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("MyLog", "Ошибка планирования будильника", e)
            // Fallback без точного времени
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    // Понедельник 8:30 — итоги недели
    fun scheduleWeekSummary(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_WEEK_SUMMARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_WEEK_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Находим ближайший понедельник 8:30
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Если понедельник уже прошёл на этой неделе — на следующую
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(REQUEST_MOTIVATION, REQUEST_REMINDER).forEach { code ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}