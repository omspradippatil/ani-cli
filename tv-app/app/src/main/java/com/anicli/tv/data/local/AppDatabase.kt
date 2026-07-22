package com.anicli.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anicli.tv.data.local.dao.FavoriteDao
import com.anicli.tv.data.local.dao.WatchProgressDao
import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.local.entity.WatchProgressEntity

@Database(
    entities = [
        WatchProgressEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun favoriteDao(): FavoriteDao
}
