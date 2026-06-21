package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.ui.composables.BitPigeonNavigationBar
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import kotlinx.coroutines.launch

@Composable
fun MainView(
    navController: NavController,
    systemViewModel: AppSystemViewModel = hiltViewModel()
) {
    // Collect the flow into a State object that Compose understands
    val isWifiEnabled by systemViewModel.isWifiEnabled.collectAsState()

    MainViewContent(
        navController = navController,
        isWifiEnabled = isWifiEnabled
    )
}

@Composable
fun MainViewContent(
    navController: NavController,
    isWifiEnabled: Boolean,
    // Hoisting sub-views as parameters allows providing mocks/placeholders in Previews
    chatGroupListView: @Composable () -> Unit = {
        ChatGroupListView(
            navController,
            chatGroupListViewModel = hiltViewModel()
        )
    },
    discoverView: @Composable () -> Unit = {
        DiscoverView(
            navController,
            discoverViewModel = hiltViewModel()
        )
    },
    profileView: @Composable () -> Unit = {
        ProfileView(
            onEditClick = {
                navController.navigate("profile_edit")
            },
            viewModel = hiltViewModel()
        )
    }
) {
    // 1. Pager State for Swiping (0 = Chats, 1 = Settings)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ViewHeader(
                title = when (pagerState.currentPage) {
                    0 -> "BitPigeon"
                    1 -> "Discover"
                    2 -> "Profile"
                    else -> "BitPigeon"
                },
                subtitle = if (pagerState.currentPage == 0 && isWifiEnabled) "Wi-Fi Direct Messaging" else null,
                showNavigationIcon = false, // No back button on main screen
                showOptionsIcon = true,
                optionsMenu = { onDismiss ->
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onDismiss()
                            navController.navigate("settings")
                        }
                    )
                }
            )
        },
        bottomBar = {
            BitPigeonNavigationBar(
                currentRoute = when (pagerState.currentPage) {
                    0 -> "chats_screen"
                    1 -> "discover_screen"
                    2 -> "profile_screen"
                    else -> "chats_screen"
                },
                onNavigate = { route ->
                    val targetPage = when (route) {
                        "chats_screen" -> 0
                        "discover_screen" -> 1
                        "profile_screen" -> 2
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
                0 -> chatGroupListView()
                1 -> discoverView()
                2 -> profileView()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainViewPreview() {
    MaterialTheme {
        // Use MainViewContent in Preview to provide dummy data and avoid ViewModel instantiation
        MainViewContent(
            navController = NavController(LocalContext.current),
            isWifiEnabled = true,
            chatGroupListView = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chat List Placeholder")
                }
            },
            discoverView = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Discover Placeholder")
                }
            },
            profileView = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Profile Placeholder")
                }
            }
        )
    }
}
