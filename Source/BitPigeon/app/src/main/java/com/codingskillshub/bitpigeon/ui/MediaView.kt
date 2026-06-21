package com.codingskillshub.bitpigeon.ui

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AttachmentViewModel

@Composable
fun MediaView(
    initialMediaId: String,
    navController: NavController,
    attachmentViewModel: AttachmentViewModel
) {

    val allMedia by attachmentViewModel.allMedia.collectAsStateWithLifecycle()
    MediaViewContent(
        initialMediaId = initialMediaId,
        allMedia = allMedia,
        onBackClick = {
            navController.popBackStack()
        }
    )
}

@Composable
fun MediaViewContent(
    initialMediaId: String,
    allMedia: List<AttachmentPreviewData>,
    onBackClick: () -> Unit = {}
) {
    if (allMedia.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Show something if empty
        }
        return
    }

    val initialIndex = remember(initialMediaId, allMedia) {
        val index = allMedia.indexOfFirst { it.id == initialMediaId }
        if (index == -1) 0 else index
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) {
        allMedia.size
    }

    val currentMedia = allMedia.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            ViewHeader(
                title = currentMedia?.fileName ?: "Media",
                onNavigationClick = onBackClick,
                showOptionsIcon = false
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
                key = { allMedia[it].id }
            ) { page ->
                val media = allMedia[page]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.fileType.startsWith("image")) {
                        AsyncImage(
                            model = media.fileUri,
                            contentDescription = media.fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else if (media.fileType.startsWith("video")) {
                        VideoPlayer(
                            uri = media.fileUri,
                            modifier = Modifier.fillMaxSize(),
                            isPlaying = pagerState.currentPage == page
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(uri: Uri, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Create and remember an ExoPlayer instance for the given uri
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    // Release the player when this composable leaves composition
    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Play or pause based on isPlaying
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                // show buffering indicator only when playing
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { view ->
            if (view.player !== exoPlayer) view.player = exoPlayer
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MediaViewContentPreview() {
    // Preview would require mocked data and Uri
}
