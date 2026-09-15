package ru.nikita.yourupgrade.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.nikita.yourupgrade.data.local.entity.SkillCategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY id")
    fun getAllCategories(): Flow<List<SkillCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<SkillCategoryEntity>)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): SkillCategoryEntity?

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAllCategoriesSync(): List<SkillCategoryEntity>
}