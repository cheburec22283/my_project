package com.fooddelivery.data.local.dao

import androidx.room.*
import com.fooddelivery.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    /** Последние 10 запросов в обратном хронологическом порядке. */
    @Query("SELECT `query` FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getHistory(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity): Long

    /** При превышении лимита удаляем самые старые записи (оставляем последние 10). */
    @Query("""
        DELETE FROM search_history 
        WHERE `query` NOT IN (
            SELECT `query` FROM search_history 
            ORDER BY timestamp DESC LIMIT 10
        )
    """)
    suspend fun trimOldEntries(): Int

    @Query("DELETE FROM search_history")
    suspend fun clearAll(): Int
}
