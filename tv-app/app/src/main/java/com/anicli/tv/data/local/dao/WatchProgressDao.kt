package com.anicli.tv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anicli.tv.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress ORDER BY lastUpdated DESC")
    fun getAllProgress(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getProgressForMedia(mediaId: String): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE mediaId = :mediaId")
    suspend fun deleteProgress(mediaId: String)
}
