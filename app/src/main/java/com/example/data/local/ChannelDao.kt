package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    fun getChannelsForPlaylist(playlistId: Int): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND streamType = :streamType")
    fun getChannelsByType(playlistId: Int, streamType: String): Flow<List<Channel>>

    @Query("SELECT DISTINCT categoryName FROM channels WHERE playlistId = :playlistId AND streamType = :streamType")
    fun getCategoriesByType(playlistId: Int, streamType: String): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND streamType = :streamType AND categoryName = :categoryName")
    fun getChannelsByCategory(playlistId: Int, streamType: String, categoryName: String): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: Int)
}
