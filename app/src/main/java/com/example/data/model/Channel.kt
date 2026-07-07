package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val name: String,
    val streamUrl: String,
    val streamType: String, // "LIVE", "MOVIE", "SERIES"
    val categoryName: String, // e.g. "Sports", "News"
    val iconUrl: String = "",
    val streamId: String = "",
    val containerExtension: String = "ts"
)
