package ru.nikita.yourupgrade.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import ru.nikita.yourupgrade.data.local.entity.AchievementEntity
import ru.nikita.yourupgrade.data.model.AchievementType
import ru.nikita.yourupgrade.databinding.DialogAchievementBinding
import java.util.concurrent.TimeUnit

class AchievementDialogFragment : DialogFragment() {

    private var _binding: DialogAchievementBinding? = null
    private val binding get() = _binding!!

    var achievements: List<AchievementEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAchievementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val achievement = achievements.firstOrNull()
        if (achievement != null) {
            setupContent(achievement)
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Запускаем конфетти после показа
        binding.konfettiView.post {
            launchConfetti()
        }
    }

    private fun setupContent(achievement: AchievementEntity) {
        val (title, description, icon) = getAchievementInfo(achievement)

        binding.tvAchievementIcon.text = icon
        binding.tvAchievementTitle.text = title
        binding.tvAchievementDescription.text = description
        binding.tvAchievementPoints.text = "+${achievement.points} очков"
    }

    private fun launchConfetti() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xFCE18A, 0xFF726D, 0xF4306D, 0xB48DEF, 0x4CAF50, 0xD5A72D),
            emitter = Emitter(duration = 500, TimeUnit.MILLISECONDS).max(150),
            position = Position.Relative(
                x = 0.5,
                y = 0.3
            )
        )
        binding.konfettiView.start(party)
    }

    private fun getAchievementInfo(achievement: AchievementEntity): Triple<String, String, String> {
        return when (achievement.type) {
            AchievementType.FULL_DAYS -> {
                val thresholds = listOf(3, 10, 30)
                Triple(
                    "Полный день ×${thresholds[achievement.level - 1]}",
                    "Все привычки выполнены за день",
                    "🏆"
                )
            }

            AchievementType.FULL_DAYS_STREAK -> {
                val thresholds = listOf(3, 7, 15)
                Triple(
                    "Серия полных дней ×${thresholds[achievement.level - 1]}",
                    "Полные дни подряд без пропусков",
                    "🔥"
                )
            }

            AchievementType.HABIT_STREAK -> {
                val thresholds = listOf(5, 15, 21)
                Triple(
                    "Мастер привычки ×${thresholds[achievement.level - 1]}",
                    "Привычка выполнена много дней подряд",
                    "💪"
                )
            }

            AchievementType.APP_USAGE -> {
                val thresholds = listOf(7, 21, 60)
                Triple(
                    "Постоянный пользователь ×${thresholds[achievement.level - 1]}",
                    "Ежедневный заход в приложение",
                    "📱"
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}