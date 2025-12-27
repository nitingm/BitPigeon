package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.codingskillshub.bitpigeon.models.ChatData
import com.codingskillshub.bitpigeon.ui.composables.BitPigeonNavigationBar
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import kotlinx.coroutines.launch

@Composable
fun MainView() {
    // 1. Pager State for Swiping (0 = Chats, 1 = Settings)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Search state for the ChatListView
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ViewHeader(
                title = if (pagerState.currentPage == 0) "BitPigeon" else "Settings",
                subtitle = if (pagerState.currentPage == 0) "Wi-Fi Direct Messaging" else null,
                showNavigationIcon = false, // No back button on main screen
                showOptionsIcon = true
            )
        },
        bottomBar = {
            BitPigeonNavigationBar(
                currentRoute = if (pagerState.currentPage == 0) "chats_screen" else "settings_screen",
                onNavigate = { route ->
                    val targetPage = if (route == "chats_screen") 0 else 1
                    scope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            )
        }
    ) { innerPadding ->
        // 2. HorizontalPager enables the left/right swipe logic
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            when (pageIndex) {
                0 -> {
                    // Page 1: Chat List
                    ChatListView(
                        chatList = getSampleChats(),
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onChatClick = { chat ->
                            // Handle navigation to ChatView
                        }
                    )
                }

                1 -> {
                    // Page 2: Settings (Placeholder for now)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Settings Content Goes Here")
                    }
                }
            }
        }
    }
}

// Helper to provide dummy data for the preview
private fun getSampleChats() = listOf(
    ChatData("1", "Aman Gupta", "Is the Wi-Fi connected?", "27/12/2025"),
    ChatData("2", "John Doe", "Sent the zip file.", "26/12/2025"),
    ChatData("3", "Dev Team", "K2 compiler is fast!", "25/12/2025")
)

@Preview(showBackground = true)
@Composable
fun MainViewPreview() {
    MaterialTheme {
        MainView()
    }
}