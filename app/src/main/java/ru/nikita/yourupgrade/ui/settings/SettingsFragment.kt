package ru.nikita.yourupgrade.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.alarm.AlarmScheduler
import ru.nikita.yourupgrade.databinding.FragmentSettingsBinding
import ru.nikita.yourupgrade.util.PreferencesManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PreferencesManager(requireContext())

        setupNotifications()
        setupSoundAndVibration()
        setupTheme()
        setupAbout()
    }

    // 🆕 Карточка 1: Уведомления
    private fun setupNotifications() {
        // Инициализация свитчей
        binding.switchMotivation.isChecked = prefs.isMotivationNotificationEnabled
        binding.switchEvening.isChecked = prefs.isEveningNotificationEnabled
        binding.switchWeekly.isChecked = prefs.isWeeklyNotificationEnabled

        // Обработчики
        binding.switchMotivation.setOnCheckedChangeListener { _, isChecked ->
            prefs.isMotivationNotificationEnabled = isChecked
            if (isChecked) {
                AlarmScheduler.scheduleMotivation(requireContext())
            } else {
                AlarmScheduler.cancelMotivation(requireContext())
            }
        }

        binding.switchEvening.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEveningNotificationEnabled = isChecked
            if (isChecked) {
                AlarmScheduler.scheduleReminder(requireContext())
            } else {
                AlarmScheduler.cancelReminder(requireContext())
            }
        }

        binding.switchWeekly.setOnCheckedChangeListener { _, isChecked ->
            prefs.isWeeklyNotificationEnabled = isChecked
            if (isChecked) {
                AlarmScheduler.scheduleWeekSummary(requireContext())
            } else {
                AlarmScheduler.cancelWeekSummary(requireContext())
            }
        }
    }

    // 🆕 Карточка 2: Звуки и вибрация
    private fun setupSoundAndVibration() {
        binding.switchSound.isChecked = prefs.isSoundEnabled
        binding.switchVibration.isChecked = prefs.isVibrationEnabled

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.isSoundEnabled = isChecked
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.isVibrationEnabled = isChecked
        }
    }

    // 🆕 Карточка 3: Тема
    private fun setupTheme() {
        // Инициализация радио-кнопок
        when (prefs.themeMode) {
            "light" -> binding.radioLight.isChecked = true
            "dark" -> binding.radioDark.isChecked = true
            else -> binding.radioSystem.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioLight -> {
                    prefs.themeMode = "light"
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                R.id.radioDark -> {
                    prefs.themeMode = "dark"
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                R.id.radioSystem -> {
                    prefs.themeMode = "system"
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }

    // 🆕 Карточка 4: О приложении
    private fun setupAbout() {
        val packageInfo = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)

        val versionName = packageInfo.versionName ?: "0.0.1"

        binding.tvVersion.text = "Версия $versionName"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}