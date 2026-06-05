package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupDb
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.ui.composables.AttachmentPreviewEntry
import com.codingskillshub.bitpigeon.ui.composables.UserEntry
import com.codingskillshub.bitpigeon.ui.composables.ViewType
import com.codingskillshub.bitpigeon.ui.viewmodels.AttachmentViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatGroupDetailView(
    navController: NavController,
    attachmentViewModel: AttachmentViewModel,
    chatViewModel: ChatViewModel
) {
    // Collect states safely with lifecycle awareness
    val chatGroup by chatViewModel.chatGroup.collectAsStateWithLifecycle()
    val users by chatViewModel.usersInGroup.collectAsStateWithLifecycle()
    val photos by attachmentViewModel.photosInChatGroup.collectAsStateWithLifecycle()
    val videos by attachmentViewModel.videosInChatGroup.collectAsStateWithLifecycle()
    val files by attachmentViewModel.filesInChatGroup.collectAsStateWithLifecycle()

    ChatGroupDetailViewContent(
        chatGroup = chatGroup,
        users = users,
        photos = photos,
        videos = videos,
        files = files,
        onBackClick = {
            navController.popBackStack()
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupDetailViewContent(
    chatGroup: ChatGroup?,
    users: List<User>,
    photos: List<AttachmentPreviewData>,
    videos: List<AttachmentPreviewData>,
    files: List<AttachmentPreviewData>,
    onBackClick: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Profile Picture in a circular container
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (chatGroup?.group?.type == ChatGroupType.GROUP)
                            Icons.Default.Group else Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 2. GroupName below it
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = chatGroup?.group?.name ?: "Unknown Group",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // 3. Status section
            val status = if (chatGroup?.group?.type == ChatGroupType.GROUP) {
                "${users.size} members"
            } else {
                users.firstOrNull { it.id != "me" }?.status ?: "Hey there! I am using BitPigeon"
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 4. Members section showing a list of Users
            Text(
                text = "Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            users.forEach { user ->
                UserEntry(user = user)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // 5. Attachments section
            Text(
                text = "Media, links and docs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            val tabs = listOf("Photos", "Videos", "Files")
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            // Swipable view (Pager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> PhotosGrid(photos)
                    1 -> VideosGrid(videos)
                    2 -> FilesList(files)
                }
            }
        }
    }
}

@Composable
private fun PhotosGrid(photos: List<AttachmentPreviewData>) {
    if (photos.isEmpty()) {
        EmptyState("No photos shared")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(photos) { photo ->
                AttachmentPreviewEntry(
                    fileName = photo.fileName,
                    fileType = photo.fileType,
                    fileUri = photo.fileUri.toString(),
                    viewType = ViewType.GRID,
                    showFileName = false
                )
            }
        }
    }
}

@Composable
private fun VideosGrid(videos: List<AttachmentPreviewData>) {
    if (videos.isEmpty()) {
        EmptyState("No videos shared")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(videos) { video ->
                AttachmentPreviewEntry(
                    fileName = video.fileName,
                    fileType = video.fileType,
                    fileUri = video.fileUri.toString(),
                    viewType = ViewType.GRID,
                    showFileName = false
                )
            }
        }
    }
}

@Composable
private fun FilesList(files: List<AttachmentPreviewData>) {
    if (files.isEmpty()) {
        EmptyState("No files shared")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            files.forEach { file ->
                AttachmentPreviewEntry(
                    fileName = file.fileName,
                    fileType = file.fileType,
                    fileUri = file.fileUri.toString(),
                    viewType = ViewType.LIST,
                    showFileName = true
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun ChatGroupDetailViewPreview() {
    val dummyGroup = ChatGroup(
        group = ChatGroupDb(id = "1", name = "BitPigeon Developers", type = ChatGroupType.GROUP),
        members = emptyList()
    )
    val dummyUsers = listOf(
        User("1", "Aman Gupta", "addr1", "123", "aman@mail.com", "", "Working on it!"),
        User("2", "John Doe", "addr2", "456", "nitin@mail.com", "", "Available")
    )
    MaterialTheme {
        ChatGroupDetailViewContent(
            chatGroup = dummyGroup,
            users = dummyUsers,
            photos = emptyList(),
            videos = emptyList(),
            files = emptyList()
        )
    }
}
