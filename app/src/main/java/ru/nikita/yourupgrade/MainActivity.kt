package ru.nikita.yourupgrade

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import ru.nikita.yourupgrade.alarm.PenaltyChecker
import ru.nikita.yourupgrade.databinding.ActivityMainBinding
import ru.nikita.yourupgrade.util.PreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        requestNotificationPermission()
        setupNavigation()
        setupFab()

        PenaltyChecker.checkAndApplyPenalties(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // 🆕 Следим за изменениями в навигации, чтобы управлять подсветкой
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // ID табов нижнего меню
            val tabIds = setOf(
                R.id.habitsFragment,
                R.id.historyFragment,
                R.id.rewardsFragment,
                R.id.settingsFragment
            )

            if (destination.id !in tabIds) {
                // 🆕 Если мы НЕ на табе — снимаем выделение со всех пунктов меню
                clearBottomNavigationSelection()
            }
        }
    }

    private fun setupFab() {
        binding.userFab.setOnClickListener {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as NavHostFragment
            val navController = navHostFragment.navController

            // 🆕 Опции навигации:
            // - popUpTo: возвращаемся к началу графа (очищаем стек от предыдущих фрагментов)
            // - launchSingleTop: не создаём дубликат userFragment если он уже сверху
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, false)  // Оставляем корень в стеке
                .setLaunchSingleTop(true)           // Не создавать дубликат
                .build()

            navController.navigate(R.id.userFragment, null, navOptions)
        }
    }

    private fun clearBottomNavigationSelection() {
        val menu = binding.bottomNavigation.menu

        // Временно отключаем "exclusive" режим, чтобы можно было снять все выделения
        menu.setGroupCheckable(0, true, false)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = false
        }
        // Включаем обратно
        menu.setGroupCheckable(0, true, true)
    }
}