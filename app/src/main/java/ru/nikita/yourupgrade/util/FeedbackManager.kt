package ru.nikita.yourupgrade.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ru.nikita.yourupgrade.R

object FeedbackManager {

    private var soundPool: SoundPool? = null
    private var successSoundId: Int = -1
    private var isLoaded = false

    private const val VIBRATE_DURATION = 300L

    private lateinit var prefs: PreferencesManager
    private lateinit var vibrator: Vibrator

    // Инициализация при старте приложения
    fun init(context: Context) {
        prefs = PreferencesManager(context)

        // Создаём SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Загружаем звук успеха
        successSoundId = soundPool!!.load(context, R.raw.success_sound, 1)
        soundPool!!.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }

        // Получаем Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Вызывается при отметке привычки
    fun onHabitMarked() {
        playSoundIfEnabled()
        vibrateIfEnabled()
    }

    // 🆕 Звук при снятии отметки (более тихий)
  /*  fun onHabitUnmarked(context: Context) {
        if (prefs.isSoundEnabled && isLoaded) {
            soundPool?.play(successSoundId, 0.5f, 0.5f, 1, 0, 1.2f)  // громкость 50%, тон выше
        }
        if (prefs.isVibrationEnabled) {
            vibrate(70)  // вибрация
        }
    }*/

    // 🆕 Звук при повышении уровня
/*    fun onLevelUp() {
        if (prefs.isSoundEnabled && isLoaded) {
            soundPool?.play(successSoundId, 1.0f, 1.0f, 1, 0, 1.5f)  // тон выше
        }
        if (prefs.isVibrationEnabled) {
            vibratePattern(longArrayOf(0, 200, 100, 200))
        }
    }*/

    // 🆕 Звук при получении ачивки
  /*  fun onAchievementEarned() {
        if (prefs.isSoundEnabled && isLoaded) {
            soundPool?.play(successSoundId, 1.0f, 1.0f, 1, 0, 1.3f)
        }
        if (prefs.isVibrationEnabled) {
            vibratePattern(longArrayOf(0, 150, 100, 150, 100, 300))
        }
    }*/

    private fun playSoundIfEnabled() {
        if (prefs.isSoundEnabled && isLoaded) {
            soundPool?.play(successSoundId, 0.1f, 0.1f, 1, 0, 1.0f)
        }
    }

    private fun vibrateIfEnabled() {
        if (prefs.isVibrationEnabled) {
            vibrate()
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(VIBRATE_DURATION, VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATE_DURATION)
        }
    }

    // Освобождение ресурсов
    fun release() {
        soundPool?.release()
        soundPool = null
    }
}