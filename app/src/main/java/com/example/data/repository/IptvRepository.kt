package com.example.data.repository

import com.example.data.local.PlaylistDao
import com.example.data.local.ChannelDao
import com.example.data.local.FavoriteDao
import com.example.data.model.Playlist
import com.example.data.model.Channel
import com.example.data.model.Favorite
import com.example.data.network.XtreamClient
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class IptvRepository(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao
) {
    private val xtreamClient = XtreamClient()
    private val httpClient = OkHttpClient()

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val allFavorites: Flow<List<Favorite>> = favoriteDao.getAllFavorites()

    suspend fun getPlaylistById(id: Int): Playlist? = withContext(Dispatchers.IO) {
        playlistDao.getPlaylistById(id)
    }

    suspend fun addPlaylist(playlist: Playlist): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsForPlaylist(playlist.id)
        playlistDao.deletePlaylist(playlist)
    }

    fun getChannelsForPlaylist(playlistId: Int): Flow<List<Channel>> =
        channelDao.getChannelsForPlaylist(playlistId)

    fun getChannelsByType(playlistId: Int, type: String): Flow<List<Channel>> =
        channelDao.getChannelsByType(playlistId, type)

    fun getCategoriesByType(playlistId: Int, type: String): Flow<List<String>> =
        channelDao.getCategoriesByType(playlistId, type)

    fun getChannelsByCategory(playlistId: Int, type: String, category: String): Flow<List<Channel>> =
        channelDao.getChannelsByCategory(playlistId, type, category)

    fun getFavoritesForPlaylist(playlistId: Int): Flow<List<Favorite>> =
        favoriteDao.getFavoritesForPlaylist(playlistId)

    suspend fun isFavorite(playlistId: Int, streamUrl: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(playlistId, streamUrl)
    }

    suspend fun toggleFavorite(playlistId: Int, channel: Channel) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavorite(playlistId, channel.streamUrl)) {
            favoriteDao.deleteFavorite(playlistId, channel.streamUrl)
        } else {
            favoriteDao.insertFavorite(
                Favorite(
                    playlistId = playlistId,
                    channelName = channel.name,
                    streamUrl = channel.streamUrl,
                    streamType = channel.streamType,
                    categoryName = channel.categoryName,
                    iconUrl = channel.iconUrl,
                    streamId = channel.streamId
                )
            )
        }
    }

    suspend fun toggleFavorite(playlistId: Int, favorite: Favorite) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavorite(playlistId, favorite.streamUrl)) {
            favoriteDao.deleteFavorite(playlistId, favorite.streamUrl)
        } else {
            favoriteDao.insertFavorite(favorite)
        }
    }

    suspend fun syncPlaylist(playlistId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext Result.failure(Exception("Playlist not found"))
            
            // Delete old channels to avoid stale cache
            channelDao.deleteChannelsForPlaylist(playlistId)

            val channels = when (playlist.type) {
                "XTREAM" -> {
                    xtreamClient.fetchChannels(
                        playlistId = playlist.id,
                        baseUrl = playlist.url,
                        username = playlist.username,
                        password = playlist.password
                    )
                }
                "M3U", "M3U8_URL", "MPD_URL" -> {
                    val request = Request.Builder().url(playlist.url).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Failed to download: $response")
                        val content = response.body?.string() ?: ""
                        if (content.contains("#EXTM3U")) {
                            M3uParser.parse(content, playlistId)
                        } else {
                            emptyList()
                        }
                    }
                }
                else -> emptyList()
            }

            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
                playlistDao.updatePlaylist(playlist.copy(lastUpdated = System.currentTimeMillis()))
                Result.success(Unit)
            } else {
                if (playlist.type == "M3U8_URL" || playlist.type == "MPD_URL" || playlist.url.endsWith(".m3u8") || playlist.url.endsWith(".mpd")) {
                    val fallbackChannel = Channel(
                        playlistId = playlistId,
                        name = playlist.name,
                        streamUrl = playlist.url,
                        streamType = "LIVE",
                        categoryName = "Direct Play"
                    )
                    channelDao.insertChannels(listOf(fallbackChannel))
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("No streams found or failed to parse."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testXtreamLogin(baseUrl: String, username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        xtreamClient.login(baseUrl, username, password)
    }
}
