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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.models.ChatData
import com.codingskillshub.bitpigeon.ui.composables.BitPigeonNavigationBar
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import kotlinx.coroutines.launch

@Composable
fun MainView(
    navController: NavController,
    systemViewModel: AppSystemViewModel
) {
    // 1. Pager State for Swiping (0 = Chats, 1 = Settings)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // Collect the flow into a State object that Compose understands
    val isWifiEnabled by systemViewModel.isWifiEnabled.collectAsState()

    Scaffold(
        topBar = {
            ViewHeader(
                title = when (pagerState.currentPage) {
                    0 -> "BitPigeon"
                    1 -> "Discover"
                    2 -> "Settings"
                    else -> "BitPigeon"
                },
                subtitle = if (pagerState.currentPage == 0 && isWifiEnabled) "Wi-Fi Direct Messaging" else null,
                showNavigationIcon = false, // No back button on main screen
                showOptionsIcon = true
            )
        },
        bottomBar = {
            BitPigeonNavigationBar(
                currentRoute = when (pagerState.currentPage) {
                    0 -> "chats_screen"
                    1 -> "discover_screen"
                    2 -> "settings_screen"
                    else -> "chats_screen"
                },
                onNavigate = { route ->
                    val targetPage = when (route) {
                        "chats_screen" -> 0
                        "discover_screen" -> 1
                        "settings_screen" -> 2
                        else -> 0
                    }
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
                        onChatClick = { chat ->
                            // Handle navigation to ChatView
                            navController.navigate("chatview/${chat.id}")
                        }
                    )
                }

                1 -> {
                    // Page 2: Discover (Placeholder for now)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Discover Content Goes Here")
                    }
                }

                2 -> {
                    // Page 3: Settings (Placeholder for now)
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
        MainView(navController = NavController(LocalContext.current),
                systemViewModel = viewModel())
    }
}