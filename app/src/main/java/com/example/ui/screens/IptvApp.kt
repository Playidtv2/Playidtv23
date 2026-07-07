package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Playlist
import com.example.data.model.Channel
import com.example.data.model.Favorite
import com.example.ui.components.VideoPlayer
import com.example.ui.viewmodel.IptvViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvApp(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val filteredFavorites by viewModel.filteredFavorites.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val playingChannel by viewModel.playingChannel.collectAsState()
    val playingFavorite by viewModel.playingFavorite.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var isFullscreenPlayer by remember { mutableStateOf(false) }

    // Apply the Elegant Dark theme to match the beautiful custom layout
    val darkColors = darkColorScheme(
        primary = Color(0xFFD0BCFF), // Elegant Light Purple
        secondary = Color(0xFF938F99), // Slate outline helper
        tertiary = Color(0xFF381E72), // Rich Dark Purple
        background = Color(0xFF1C1B1F), // Elegant Pure Dark
        surface = Color(0xFF2B2930), // Premium Dark Card Background
        outline = Color(0xFF49454F), // Subtle divider/border color
        onBackground = Color(0xFFE6E1E5), // Soft High Contrast White
        onSurface = Color(0xFFE6E1E5)
    )

    MaterialTheme(
        colorScheme = darkColors
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isFullscreenPlayer) {
                    // Fullscreen Video Player
                    val currentStreamUrl = playingChannel?.streamUrl ?: playingFavorite?.streamUrl ?: ""
                    val currentTitle = playingChannel?.name ?: playingFavorite?.channelName ?: "IPTV Stream"
                    
                    if (currentStreamUrl.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoPlayer(
                                videoUrl = currentStreamUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlaid full screen back button & Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(16.dp)
                                    .statusBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isFullscreenPlayer = false }) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Minimize Screen",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentTitle,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Normal Split View Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Top Bar Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                                .statusBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedPlaylist != null) {
                                    IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back to Playlists",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "IPTV Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedPlaylist?.name ?: "IPTV PLAY",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            if (selectedPlaylist == null) {
                                Button(
                                    onClick = {
                                        playlistToEdit = null
                                        showAddDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Playlist")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("เพิ่มเพลย์ลิสต์", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.syncPlaylist(selectedPlaylist!!.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Sync Playlist",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Exit Playlist",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Split Screen Player (Show at top if active and in split view)
                        val activeUrl = playingChannel?.streamUrl ?: playingFavorite?.streamUrl ?: ""
                        if (activeUrl.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .background(Color.Black)
                            ) {
                                VideoPlayer(
                                    videoUrl = activeUrl,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Overlaid panel controllers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = playingChannel?.name ?: playingFavorite?.channelName ?: "",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = { isFullscreenPlayer = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "Fullscreen Player",
                                                tint = Color.White
                                            )
                                        }
                                        IconButton(onClick = { viewModel.stopPlayer() }) {
                                            Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "Stop Stream",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedPlaylist == null) {
                            // Show Playlists Selection Manager
                            PlaylistsScreen(
                                playlists = playlists,
                                onSelect = { viewModel.selectPlaylist(it) },
                                onDelete = { viewModel.deletePlaylist(it) },
                                onEdit = {
                                    playlistToEdit = it
                                    showAddDialog = true
                                },
                                onSync = { viewModel.syncPlaylist(it.id) }
                            )
                        } else {
                            // Main Channels Browse Dashboard
                            DashboardScreen(
                                selectedType = selectedType,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                channels = channels,
                                favorites = filteredFavorites,
                                categories = categories,
                                onTypeSelected = { viewModel.selectType(it) },
                                onCategorySelected = { viewModel.selectCategory(it) },
                                onSearchChanged = { viewModel.setSearchQuery(it) },
                                onChannelSelected = { viewModel.playChannel(it) },
                                onFavoriteSelected = { viewModel.playFavorite(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                isFavorite = { viewModel.isFavorite(it) }
                            )
                        }
                    }
                }

                // Sync Loading / Success dialog layer
                when (syncState) {
                    is IptvViewModel.SyncStatus.Syncing -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.width(300.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "กำลังซิงค์และดาวน์โหลดข้อมูล...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "เชื่อมต่อ Xtream API / ดาวน์โหลด m3u playlist ของคุณลงในเครื่อง",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    is IptvViewModel.SyncStatus.Success -> {
                        LaunchedEffect(syncState) {
                            delay(1200)
                            viewModel.clearSyncState()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color.Green,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "ซิงค์ข้อมูลสำเร็จแล้ว!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    is IptvViewModel.SyncStatus.Error -> {
                        AlertDialog(
                            onDismissRequest = { viewModel.clearSyncState() },
                            title = { Text("การซิงค์ล้มเหลว", fontWeight = FontWeight.Bold) },
                            text = { Text((syncState as IptvViewModel.SyncStatus.Error).message) },
                            confirmButton = {
                                TextButton(onClick = { viewModel.clearSyncState() }) {
                                    Text("ตกลง", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                    else -> {}
                }

                // Add / Edit Playlist Dialog Modal
                if (showAddDialog) {
                    AddPlaylistDialog(
                        playlist = playlistToEdit,
                        onDismiss = { showAddDialog = false },
                        onSave = { name, type, url, user, pass ->
                            viewModel.savePlaylist(
                                id = playlistToEdit?.id ?: 0,
                                name = name,
                                type = type,
                                url = url,
                                username = user,
                                password = pass
                            )
                            showAddDialog = false
                        }
                    )
                }
            }
        }
    }
}

// Playlists Selection Layout
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onSelect: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onEdit: (Playlist) -> Unit,
    onSync: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = "No Playlists",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ยังไม่มีข้อมูลเพลย์ลิสต์",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "กดปุ่มเพิ่มเพลย์ลิสต์ ด้านบนเพื่อเข้าสู่ระบบ Xtream Codes API หรือป้อนเพลย์ลิสต์ M3U/M3U8/MPD เพื่อเริ่มต้นใช้งาน",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "เพลย์ลิสต์ทั้งหมดของคุณ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onSelect = { onSelect(playlist) },
                    onDelete = { onDelete(playlist) },
                    onEdit = { onEdit(playlist) },
                    onSync = { onSync(playlist) }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (playlist.type) {
                        "XTREAM" -> MaterialTheme.colorScheme.primary
                        "M3U" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = playlist.type,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = onSync) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "เซิร์ฟเวอร์/ลิงก์: ${playlist.url}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (playlist.type == "XTREAM") {
                Text(
                    text = "ผู้ใช้งาน: ${playlist.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val dateStr = format.format(Date(playlist.lastUpdated))
            Text(
                text = "อัพเดทล่าสุดเมื่อ: $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Add/Edit Dialog Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistDialog(
    playlist: Playlist?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, url: String, user: String, pass: String) -> Unit
) {
    var name by remember { mutableStateOf(playlist?.name ?: "") }
    var type by remember { mutableStateOf(playlist?.type ?: "XTREAM") }
    var url by remember { mutableStateOf(playlist?.url ?: "") }
    var username by remember { mutableStateOf(playlist?.username ?: "") }
    var password by remember { mutableStateOf(playlist?.password ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (playlist == null) "เพิ่มเพลย์ลิสต์" else "แก้ไขเพลย์ลิสต์",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Playlist Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อเพลย์ลิสต์") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val typesList = listOf("XTREAM", "M3U")
                    typesList.forEach { item ->
                        val isSelected = type == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { type = item }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (item == "XTREAM") "Xtream API" else "M3U Link",
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Server URL / M3U Link
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (type == "XTREAM") "เซิร์ฟเวอร์ URL (เช่น http://host:port)" else "ลิงก์ M3U/M3U8/MPD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == "XTREAM") {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("ชื่อผู้ใช้งาน (Username)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("รหัสผ่าน (Password)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                error?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isEmpty() || url.isEmpty()) {
                                error = "กรุณากรอกข้อมูลให้ครบถ้วน"
                            } else if (type == "XTREAM" && (username.isEmpty() || password.isEmpty())) {
                                error = "กรุณากรอกชื่อผู้ใช้และรหัสผ่านสำหรับ Xtream Codes"
                            } else {
                                onSave(name, type, url, username, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("บันทึก", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dashboard Screen for Browsing Playlists Channels
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    selectedType: String,
    selectedCategory: String?,
    searchQuery: String,
    channels: List<Channel>,
    favorites: List<Favorite>,
    categories: List<String>,
    onTypeSelected: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onFavoriteSelected: (Favorite) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    isFavorite: (Channel) -> Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Types Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(
                "LIVE" to "ทีวีสด",
                "MOVIE" to "ภาพยนตร์",
                "SERIES" to "ซีรีส์",
                "FAVORITE" to "รายการโปรด"
            )
            types.forEach { (typeKey, typeLabel) ->
                val isSelected = selectedType == typeKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(typeKey) },
                    label = { Text(typeLabel) },
                    leadingIcon = {
                        val icon = when (typeKey) {
                            "LIVE" -> Icons.Default.LiveTv
                            "MOVIE" -> Icons.Default.Movie
                            "SERIES" -> Icons.Default.VideoLibrary
                            else -> Icons.Default.Favorite
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }

        // Search Bar Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("ค้นหารายการ / ช่องทีวี...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Horizontal Category Row
        if (selectedType != "FAVORITE" && categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAllSelected = selectedCategory == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { onCategorySelected(null) },
                        label = { Text("ทั้งหมด") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Channels List / Grid
        Box(modifier = Modifier.weight(1.0f)) {
            if (selectedType == "FAVORITE") {
                if (favorites.isEmpty()) {
                    EmptyListPlaceholder("ไม่มีรายการโปรด")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favorites) { fav ->
                            FavoriteCardItem(
                                favorite = fav,
                                onClick = { onFavoriteSelected(fav) }
                            )
                        }
                    }
                }
            } else {
                if (channels.isEmpty()) {
                    EmptyListPlaceholder("ไม่พบข้อมูลช่องรายการ")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(channels) { channel ->
                            ChannelCardItem(
                                channel = channel,
                                onClick = { onChannelSelected(channel) },
                                onToggleFavorite = { onToggleFavorite(channel) },
                                isFav = isFavorite(channel)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListPlaceholder(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ChannelCardItem(
    channel: Channel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFav: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Channel Logo Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.iconUrl.isNotEmpty()) {
                        AsyncImage(
                            model = channel.iconUrl,
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Channel metadata info
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite Icon overlaid
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFav) Color.Red else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FavoriteCardItem(
    favorite: Favorite,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (favorite.iconUrl.isNotEmpty()) {
                    AsyncImage(
                        model = favorite.iconUrl,
                        contentDescription = favorite.channelName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = favorite.channelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = favorite.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


