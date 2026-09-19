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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.nikita.yourupgrade.alarm.PenaltyChecker
import ru.nikita.yourupgrade.databinding.ActivityMainBinding
import ru.nikita.yourupgrade.ui.common.BottomMenu
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

      //  binding.bottomNavigation.setupWithNavController(navController)

        // 🆕 Следим за изменениями в навигации, чтобы управлять подсветкой
        // Кастомный бар вместо BottomNavigationView
        binding.bottomNavigation.onTabSelected = { position ->
            val destinationId = when (position) {
                0 -> R.id.habitsFragment
                1 -> R.id.historyFragment
                2 -> R.id.rewardsFragment
                3 -> R.id.settingsFragment
                else -> R.id.habitsFragment
            }
            navController.navigate(destinationId, null, NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, false)
                .setLaunchSingleTop(true)
                .build())


        }

        // Синхронизация при навигации назад/вперёд
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.setActiveByFragment(destination.id)
        }

        // FAB
        binding.fabUser.setOnClickListener {
            navController.navigate(R.id.userFragment, null, NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, false)
                .setLaunchSingleTop(true)
                .build())
        }
    }
}