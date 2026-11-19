package com.fooddelivery.data.local

import androidx.room.*
import com.fooddelivery.data.local.dao.SearchHistoryDao
import com.fooddelivery.data.local.entity.SearchHistoryEntity

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
}
