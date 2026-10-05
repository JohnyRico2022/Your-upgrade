package ru.nikita.yourupgrade.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nikita.yourupgrade.data.local.AppDatabase
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val _categories = MutableStateFlow<List<SkillCategoryEntity>>(emptyList())
    val categories: StateFlow<List<SkillCategoryEntity>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val categories = withContext(Dispatchers.IO) {
                db.categoryDao().getAllCategoriesSync()
            }
            _categories.value = categories
        }
    }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val category = SkillCategoryEntity(
                name = name,
                iconEmoji = emoji,
                isUserCreated = true  // 🆕 Помечаем как пользовательскую
            )
            withContext(Dispatchers.IO) {
                db.categoryDao().insertCategory(category)
            }
            loadCategories()
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.categoryDao().deleteCategory(categoryId)
            }
            loadCategories()
        }
    }
}