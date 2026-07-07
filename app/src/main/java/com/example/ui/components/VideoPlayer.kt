package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlayerError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize ExoPlayer with standard performance enhancements
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Set up player state listener
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                val desc = "Error playing stream: ${error.localizedMessage ?: "Unknown network/codec error"}"
                errorMessage = desc
                onPlayerError(desc)
            }
        })
    }

    // React to video URL change and automatically detect proper streaming formats (HLS, DASH, Progressive)
    LaunchedEffect(videoUrl) {
        isLoading = true
        errorMessage = null
        try {
            val mediaItemBuilder = MediaItem.Builder().setUri(videoUrl)
            
            // Explicitly set MimeType based on the extension to ensure optimal decoding
            when {
                videoUrl.contains(".m3u8", ignoreCase = true) || videoUrl.contains("m3u8", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                videoUrl.contains(".mpd", ignoreCase = true) || videoUrl.contains("mpd", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                videoUrl.contains(".ts", ignoreCase = true) -> {
                    // Standard MPEG-TS stream for Xtream Codes LIVE streams
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                }
            }
            
            val mediaItem = mediaItemBuilder.build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            errorMessage = "Invalid stream URL or protocol: ${e.localizedMessage}"
            isLoading = false
        }
    }

    // Clean up player on leave
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // Show standard controllers containing Play, Pause, Timeline tracking immediately
                    showController()
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
