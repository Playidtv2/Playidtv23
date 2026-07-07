package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val channelName: String,
    val streamUrl: String,
    val streamType: String,
    val categoryName: String,
    val iconUrl: String = "",
    val streamId: String = ""
)
