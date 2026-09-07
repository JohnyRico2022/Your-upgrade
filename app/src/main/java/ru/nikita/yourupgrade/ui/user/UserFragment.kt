package ru.nikita.yourupgrade.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.nikita.yourupgrade.databinding.FragmentUserBinding

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoriesList()
        observeState()
    }

    private fun setupCategoriesList() {
        categoryAdapter = CategoryAdapter()
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = categoryAdapter
        binding.rvCategories.isNestedScrollingEnabled = false
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvGreeting.text = "Привет"
                    binding.tvUserName.text = state.userName
                    binding.tvMotivation.text = state.motivationQuote

                    // Крупный уровень
                    binding.tvLevel.text = state.currentLevel.toString()

                    // Прогресс-бар
                    binding.progressLevel.progress = (state.progressToNextLevel * 100).toInt()
                    binding.tvProgressLabel.text = state.progressLabel

                    // Статистика сегодня
                    binding.tvTodayStats.text =
                        "Выполнено ${state.todayDoneCount} из ${state.todayTotalCount} привычек"

                    // Категории
                    categoryAdapter.submitList(state.categories)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}