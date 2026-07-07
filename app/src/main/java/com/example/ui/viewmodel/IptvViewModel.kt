package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Playlist
import com.example.data.model.Channel
import com.example.data.model.Favorite
import com.example.data.repository.IptvRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = IptvRepository(
        database.playlistDao(),
        database.channelDao(),
        database.favoriteDao()
    )

    val playlists = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites = repository.allFavorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Selection State
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedType = MutableStateFlow("LIVE") // "LIVE", "MOVIE", "SERIES", "FAVORITE"
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Channel Playing State
    private val _playingChannel = MutableStateFlow<Channel?>(null)
    val playingChannel: StateFlow<Channel?> = _playingChannel.asStateFlow()

    private val _playingFavorite = MutableStateFlow<Favorite?>(null)
    val playingFavorite: StateFlow<Favorite?> = _playingFavorite.asStateFlow()

    // Sync State
    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    sealed interface SyncStatus {
        object Idle : SyncStatus
        object Syncing : SyncStatus
        data class Success(val count: Int) : SyncStatus
        data class Error(val message: String) : SyncStatus
    }

    // Filtered categories & channels
    val categories: StateFlow<List<String>> = _selectedPlaylist
        .combine(_selectedType) { playlist, type ->
            if (playlist == null || type == "FAVORITE") {
                emptyList()
            } else {
                repository.getCategoriesByType(playlist.id, type).first()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = combine(
        _selectedPlaylist,
        _selectedType,
        _selectedCategory,
        _searchQuery
    ) { playlist, type, category, search ->
        if (playlist == null || type == "FAVORITE") {
            emptyList()
        } else {
            val list = if (category == null) {
                repository.getChannelsByType(playlist.id, type).first()
            } else {
                repository.getChannelsByCategory(playlist.id, type, category).first()
            }
            if (search.isEmpty()) {
                list
            } else {
                list.filter { it.name.contains(search, ignoreCase = true) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFavorites: StateFlow<List<Favorite>> = combine(
        _selectedPlaylist,
        _searchQuery,
        favorites
    ) { playlist, search, favs ->
        val list = if (playlist == null) favs else favs.filter { it.playlistId == playlist.id }
        if (search.isEmpty()) {
            list
        } else {
            list.filter { it.channelName.contains(search, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
        _selectedCategory.value = null
        _searchQuery.value = ""
        _playingChannel.value = null
        _playingFavorite.value = null
    }

    fun selectType(type: String) {
        _selectedType.value = type
        _selectedCategory.value = null
        _searchQuery.value = ""
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playChannel(channel: Channel) {
        _playingFavorite.value = null
        _playingChannel.value = channel
    }

    fun playFavorite(favorite: Favorite) {
        _playingChannel.value = null
        _playingFavorite.value = favorite
    }

    fun stopPlayer() {
        _playingChannel.value = null
        _playingFavorite.value = null
    }

    fun clearSyncState() {
        _syncState.value = SyncStatus.Idle
    }

    // Playlist Add/Edit/Delete
    fun savePlaylist(
        id: Int = 0,
        name: String,
        type: String,
        url: String,
        username: String = "",
        password: String = ""
    ) {
        viewModelScope.launch {
            val formattedUrl = if (url.endsWith("/")) url.dropLast(1) else url
            val p = Playlist(
                id = id,
                name = name,
                type = type,
                url = formattedUrl,
                username = username,
                password = password,
                lastUpdated = System.currentTimeMillis()
            )
            val newId = if (id == 0) {
                repository.addPlaylist(p).toInt()
            } else {
                repository.updatePlaylist(p)
                id
            }

            syncPlaylist(newId)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == playlist.id) {
                selectPlaylist(null)
            }
            repository.deletePlaylist(playlist)
        }
    }

    fun syncPlaylist(playlistId: Int) {
        viewModelScope.launch {
            _syncState.value = SyncStatus.Syncing
            val result = repository.syncPlaylist(playlistId)
            if (result.isSuccess) {
                val playlist = repository.getPlaylistById(playlistId)
                if (playlist != null) {
                    _selectedPlaylist.value = playlist
                }
                _syncState.value = SyncStatus.Success(0)
            } else {
                _syncState.value = SyncStatus.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val pid = channel.playlistId
            repository.toggleFavorite(pid, channel)
        }
    }

    fun toggleFavorite(favorite: Favorite) {
        viewModelScope.launch {
            repository.toggleFavorite(favorite.playlistId, favorite)
        }
    }

    fun isFavorite(channel: Channel): Boolean {
        return favorites.value.any { it.playlistId == channel.playlistId && it.streamUrl == channel.streamUrl }
    }
}
