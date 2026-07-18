package com.codingskillshub.bitpigeon.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.codingskillshub.bitpigeon.domain.services.AudioService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.ui.onboardingscreens.OnboardingMainView
import com.codingskillshub.bitpigeon.ui.settingpages.AboutView
import com.codingskillshub.bitpigeon.ui.settingpages.AppearanceView
import com.codingskillshub.bitpigeon.ui.settingpages.LanguagesView
import com.codingskillshub.bitpigeon.ui.theme.AppTheme
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.AttachmentViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatGroupListViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ProfileViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // 1. Hilt will now provide these singletons automatically
    @Inject
    lateinit var manager: WifiP2pManager
    @Inject
    lateinit var channel: WifiP2pManager.Channel
    @Inject
    lateinit var wifiService: WifiCommunicationService
    @Inject
    lateinit var audioService: AudioService

    // Inside your Activity
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
    }

    // Define the permissions needed based on Android Version
    private val permissionsToRequest: Array<String> = run {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissions.toTypedArray()
    }


    private var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Create the launcher
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                wifiService.discoverPeers()
            }
        }

        // 3. Launch the request immediately on open
        requestPermissionLauncher.launch(permissionsToRequest)

        enableEdgeToEdge()
        setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val systemViewModel: AppSystemViewModel = hiltViewModel()
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val chatGroupListViewModel: ChatGroupListViewModel = hiltViewModel()

            val isOnboardingCompleted by systemViewModel.isOnboardingCompleted.collectAsState()
            val themeState by systemViewModel.appTheme.collectAsState()

            AppTheme(selectedTheme = themeState.first) {
                if (isOnboardingCompleted != null) {
                    NavHost(
                        navController = navController,
                        startDestination = if (isOnboardingCompleted == true) "main" else "onboarding"
                    ) {
                        composable("onboarding") {
                            OnboardingMainView(navController, systemViewModel)
                        }
                        composable("main") {
                            MainView(navController, systemViewModel)
                        }
                        composable("search_group") {
                            SearchChatGroupView(navController, chatGroupListViewModel)
                        }
                        navigation(startDestination = "chatview", route = "chat") {
                            composable(
                                "chatview/{chatId}",
                                arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(500)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(500)
                                    )
                                },
                                popEnterTransition = {
                                    EnterTransition.None
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(500)
                                    )
                                }
                            ) { backStackEntry ->
                                val chatViewModel: ChatViewModel = hiltViewModel()

                                ChatView(navController, systemViewModel, chatViewModel)
                            }
                            composable(
                                "chat_group_detail/{chatId}",
                                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val attachmentViewModel: AttachmentViewModel = hiltViewModel()
                                val chatViewModel: ChatViewModel = hiltViewModel()
                                ChatGroupDetailView(navController, attachmentViewModel, chatViewModel)
                            }
                            composable(
                                "media_view/{chatId}?mediaId={mediaId}",
                                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val mediaId = backStackEntry.arguments?.getString("mediaId")
                                val attachmentViewModel: AttachmentViewModel = hiltViewModel()
                                MediaView(mediaId ?: "", navController, attachmentViewModel)
                            }
                        }
                        navigation(startDestination = "settings", route = "setting_pages") {
                            composable("settings",
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(500)
                                    )
                                },
                                popEnterTransition = {
                                    EnterTransition.None
                                }
                            ) {
                                SettingsView(navController, systemViewModel)
                            }
                            composable("about") {
                                AboutView(navController, systemViewModel)
                            }
                            composable("languages") {
                                LanguagesView(navController)
                            }
                            composable("appearance") {
                                AppearanceView(navController, systemViewModel)
                            }
                        }
                        composable("profile_edit",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = tween(500)
                                )
                            }
                        ) {
                            ProfileEditView(
                                onNavigateBack = { navController.popBackStack() },
                                profileViewModel
                            )
                        }
                    }
                } else {
                    // Show a blank surface while waiting for onboarding status to load
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        audioService.initialize() // Re-initialize audio resources
        receiver = wifiService.getWifiDirectBroadcastReceiver()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioService.release()
    }
}
