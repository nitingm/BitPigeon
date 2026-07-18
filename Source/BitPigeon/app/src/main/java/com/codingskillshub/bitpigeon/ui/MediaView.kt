package com.codingskillshub.bitpigeon.ui

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AttachmentViewModel
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

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
                        ZoomableImage(
                            uri = media.fileUri,
                            contentDescription = media.fileName
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

@Composable
fun ZoomableImage(uri: Uri, contentDescription: String?) {
    // Telephoto handles zoom, pan, and double-tap out of the box.
    // It also correctly handles the gesture hand-off with HorizontalPager.
    val zoomableState = rememberZoomableState()
    
    ZoomableAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(uri)
            .build(),
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        state = rememberZoomableImageState(zoomableState),
        contentScale = ContentScale.Fit
    )
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(uri: Uri, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        if (isPlaying) {
            // Optimization: Create and remember an ExoPlayer instance ONLY when the page is active.
            val exoPlayer = remember(uri) {
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        3000,  // minBufferMs (Default: 15000)
                        10000, // maxBufferMs (Default: 50000)
                        1000,  // bufferForPlaybackMs
                        1500   // bufferForPlaybackAfterRebufferMs
                    )
                    .build()

                ExoPlayer.Builder(context)
                    .setLoadControl(loadControl)
                    .build().apply {
                        setMediaItem(MediaItem.fromUri(uri))
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                        prepare()
                    }
            }

            // Release the player when this specific VideoPlayer (for the active page) is removed or changes.
            DisposableEffect(exoPlayer) {
                onDispose {
                    exoPlayer.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Optimization: Show a static thumbnail using Coil for non-active pages in the pager.
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .build(),
                contentDescription = "Video Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaViewContentPreview() {
    // Preview would require mocked data and Uri
}
