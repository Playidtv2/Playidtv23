package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "XTREAM" or "M3U" or "M3U8_URL" or "MPD_URL"
    val url: String, // Base server URL for Xtream, or Playlist URL for M3U/M3U8/MPD
    val username: String = "",
    val password: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
