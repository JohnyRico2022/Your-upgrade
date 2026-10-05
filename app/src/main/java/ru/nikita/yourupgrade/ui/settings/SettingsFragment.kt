package ru.nikita.yourupgrade.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.alarm.AlarmScheduler
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity
import ru.nikita.yourupgrade.databinding.FragmentSettingsBinding
import ru.nikita.yourupgrade.util.PreferencesManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PreferencesManager
    private lateinit var viewModel: SettingsViewModel

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
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setupNotifications()
        setupSoundAndVibration()
      //  setupTheme()
        setupAbout()
        setupCategories()  // 🆕

        observeCategories()  // 🆕
    }

    // Уведомления
    private fun setupNotifications() {
        binding.switchMotivation.isChecked = prefs.isMotivationNotificationEnabled
        binding.switchEvening.isChecked = prefs.isEveningNotificationEnabled
        binding.switchWeekly.isChecked = prefs.isWeeklyNotificationEnabled

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

    // Звуки и вибрация
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

    /** Тема */
    //todo Вернуть в будущих релизах

    /* private fun setupTheme() {
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
     }*/



    /** Добавление категорий */
    private fun setupCategories() {
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    // 🆕 Заполняем список категорий
    private fun fillCategoriesContainer(categories: List<SkillCategoryEntity>) {
        binding.categoriesContainer.removeAllViews()

        categories.forEach { category ->
            val categoryView = createCategoryRow(category)
            binding.categoriesContainer.addView(categoryView)
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    fillCategoriesContainer(categories)
                }
            }
        }
    }

    // 🆕 Создаём строку категории
    private fun createCategoryRow(category: SkillCategoryEntity): View {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(4), dpToPx(10), dpToPx(4), dpToPx(10))
            gravity = Gravity.CENTER_VERTICAL
        }

        // Эмодзи + название
        val labelView = TextView(context).apply {
            text = "${category.iconEmoji} ${category.name}"
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Иконка удаления (только для пользовательских категорий)
        val deleteView = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
            setOnClickListener {
                showDeleteCategoryDialog(category)
            }
        }

        container.addView(labelView)

        // Кнопку удаления показываем только для пользовательских категорий (если есть такое поле)
        if (category.isUserCreated) {
            container.addView(deleteView)
        }

        return container
    }

    // 🆕 Диалог добавления категории
    private fun showAddCategoryDialog() {
        val dialog = AddCategoryDialogFragment()
        dialog.onCategoryCreated = { name, emoji ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.addCategory(name, emoji)
            }
        }
        dialog.show(childFragmentManager, "add_category")
    }

    // 🆕 Диалог удаления категории
    private fun showDeleteCategoryDialog(category: SkillCategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить категорию?")
            .setMessage("Привычки в категории '${category.name}' останутся, но без категории.")
            .setPositiveButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteCategory(category.id)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    /** О приложении */
    private fun setupAbout() {
        val packageInfo = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)

        val versionName = packageInfo.versionName ?: "1.0.0"
        binding.tvVersion.text = getString(R.string.settings_version_format, versionName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}