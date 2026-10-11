package ru.nikita.yourupgrade.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.alarm.PenaltyChecker
import ru.nikita.yourupgrade.data.calculator.AchievementChecker
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.local.entity.AppUsageEntity
import ru.nikita.yourupgrade.databinding.ActivityMainBinding
import ru.nikita.yourupgrade.ui.rewards.AchievementDialogFragment
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
       // showTestAchievement()  // для тестов
        setupAchievements()
        PenaltyChecker.checkAndApplyPenalties(this)
    }

    /// Тестовый показ — всегда показывается при старте
    /*   private fun showTestAchievement() {
           // Задержка чтобы MainActivity успел отрисоваться
           binding.root.postDelayed({
               val testAchievement = AchievementEntity(
                   id = 0,
                   type = AchievementType.APP_USAGE,
                   level = 1,
                   earnedDate = "2026-09-27",
                   points = 2
               )

               val dialog = AchievementDialogFragment().apply {
                   this.achievements = listOf(testAchievement)
               }
               dialog.show(supportFragmentManager, "test_achievement")
           }, 500)  // 500мс задержка
       }*/

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
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.onTabSelected = { position ->
            val destinationId = when (position) {
                0 -> R.id.habitsFragment
                1 -> R.id.historyFragment
                2 -> R.id.rewardsFragment
                3 -> R.id.settingsFragment
                else -> R.id.habitsFragment
            }
            navController.navigate(
                destinationId, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, false)
                    .setLaunchSingleTop(true)
                    .build()
            )

            setFabActive(false)
        }

        // Синхронизация при навигации назад/вперёд
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.setActiveByFragment(destination.id)
        }

        // FAB
        binding.fabUser.setOnClickListener {
            navController.navigate(
                R.id.userFragment, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, false)
                    .setLaunchSingleTop(true)
                    .build()
            )

            setFabActive(true)
            binding.bottomNavigation.deActiveTab()
        }

        binding.bottomNavigation.selectTab(0)
    }

    private fun setFabActive(isActive: Boolean) {
        val colorId: Int = if (isActive) android.R.color.white else android.R.color.black

        binding.fabUser.imageTintList = ColorStateList.valueOf(
            this.getColor(colorId)
        )
    }

    private fun setupAchievements() {
        backfillAppUsage()

        // 🆕 При старте проверяем ТОЛЬКО заход в приложение
        AchievementChecker.checkAppUsage(this) { newAchievements ->
            if (newAchievements.isNotEmpty()) {
                showAchievementDialog(newAchievements)
            }
        }
    }

    private fun showAchievementDialog(achievements: List<AchievementEntity>) {
        val dialog = AchievementDialogFragment().apply {
            this.achievements = achievements
        }
        dialog.show(supportFragmentManager, "achievement")
    }


    private fun backfillAppUsage() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val allLogs = db.habitLogDao().getAllLogsSync()  // или habitLogDao()

            val uniqueDates = allLogs.map { it.date }.distinct()
            var filledCount = 0

            uniqueDates.forEach { date ->
                val existing = db.appUsageDao().getUsageByDate(date)
                if (existing == null) {
                    db.appUsageDao().insertUsage(AppUsageEntity(date = date))
                    filledCount++
                }
            }

            if (filledCount > 0) {
                Log.d("Backfill", "Добавлено $filledCount дней использования")
            }
        }
    }
}