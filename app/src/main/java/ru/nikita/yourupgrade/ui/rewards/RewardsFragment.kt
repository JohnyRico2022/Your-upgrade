package ru.nikita.yourupgrade.ui.rewards

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.nikita.yourupgrade.R
import ru.nikita.yourupgrade.databinding.FragmentRewardsBinding
import ru.nikita.yourupgrade.databinding.FragmentSettingsBinding

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvPlaceholder.text = "Награды"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}