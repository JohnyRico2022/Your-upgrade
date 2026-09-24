package ru.nikita.yourupgrade.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_LAUNCH = "first_launch"

        // Уведомления
        private const val KEY_NOTIFICATION_MOTIVATION = "notification_motivation_enabled"
        private const val KEY_NOTIFICATION_EVENING = "notification_evening_enabled"
        private const val KEY_NOTIFICATION_WEEKLY = "notification_weekly_enabled"

        // Звуки и вибрация
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        // Тема
        private const val KEY_THEME_MODE = "theme_mode"
    }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Пользователь") ?: "Пользователь"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    val isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    // Уведомления (по умолчанию включены)
    var isMotivationNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_MOTIVATION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_MOTIVATION, value).apply()

    var isEveningNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_EVENING, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_EVENING, value).apply()

    var isWeeklyNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_WEEKLY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_WEEKLY, value).apply()

    // Звуки и вибрация (по умолчанию включены)
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    // Тема: "light", "dark", "system"
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()


    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
}