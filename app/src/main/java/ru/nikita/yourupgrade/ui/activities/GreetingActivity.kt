package ru.nikita.yourupgrade.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.nikita.yourupgrade.databinding.ActivityGreetingBinding
import ru.nikita.yourupgrade.util.PreferencesManager

class GreetingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGreetingBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityGreetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // Если не первый запуск — сразу в Main
        if (!prefs.isFirstLaunch) {
            navigateToMain()
            return
        }

        binding.btnStart.setOnClickListener {
            val name = binding.etUserName.text.toString().trim()
            if (name.isNotEmpty()) {
                prefs.userName = name
                prefs.setFirstLaunchComplete()
                navigateToMain()
            } else {
                binding.etUserName.error = "Введите имя"
            }
        }
    }

    private fun applyTheme() {
        val prefs = PreferencesManager(this)
        when (prefs.themeMode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}