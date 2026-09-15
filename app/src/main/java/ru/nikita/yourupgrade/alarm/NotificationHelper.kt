package ru.nikita.yourupgrade.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.nikita.yourupgrade.MainActivity
import ru.nikita.yourupgrade.R

object NotificationHelper {

    private const val CHANNEL_ID = "habit_reminders"
    private const val CHANNEL_NAME = "Напоминания о привычках"
    private const val NOTIFICATION_ID_MOTIVATION = 1001
    private const val NOTIFICATION_ID_REMINDER = 1002

    private const val NOTIFICATION_ID_WEEK_SUMMARY = 1003

    /**
     * Создаёт канал уведомлений (нужно вызвать один раз при старте)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напоминания о выполнении привычек"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Показывает уведомление
     */
    private fun showNotification(
        context: Context,
        title: String,
        text: String,
        notificationId: Int
    ) {
        // Проверяем разрешение на уведомления (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED !=
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                return  // Нет разрешения — не показываем
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_star_24_yellow)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // На некоторых устройствах может быть запрет
            e.printStackTrace()
        }
    }

    /**
     * 15:00 — мотивационное уведомление
     */
    fun showMotivation(context: Context) {
        showNotification(
            context,
            title = "Не забывай себя прокачивать! 💪",
            text = "Каждая выполненная привычка делает тебя сильнее.",
            notificationId = NOTIFICATION_ID_MOTIVATION
        )
    }

    /**
     * 22:30 — напоминание отметить привычки
     */
    fun showReminder(context: Context) {
        showNotification(
            context,
            title = "Не забудь отметить привычки! 📝",
            text = "Отметь выполнение за текущий день, чтобы не потерять прогресс.",
            notificationId = NOTIFICATION_ID_REMINDER
        )
    }

    fun showWeekSummary(context: Context) {
        showNotification(
            context,
            title = "Итоги прошлой недели готовы! 📊",
            text = "Проверь, как прошла твоя неделя и какие привычки ты выполнил.",
            notificationId = NOTIFICATION_ID_WEEK_SUMMARY
        )
    }

}