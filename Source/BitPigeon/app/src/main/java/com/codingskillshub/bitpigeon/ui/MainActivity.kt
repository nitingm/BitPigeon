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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

import com.codingskillshub.bitpigeon.infrastructure.WifiDirectBroadcastReceiver
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
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
    // 1. Declare the Manager and Channel
    // 1. Hilt will now provide these singletons automatically
    @Inject
    lateinit var manager: WifiP2pManager
    @Inject
    lateinit var channel: WifiP2pManager.Channel
    @Inject
    lateinit var wifiService: WifiCommunicationService

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

        lifecycleScope.launch {

        }

        // 1. Create the launcher
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                wifiService.discoverPeers()
            } else {
                // Handle permission denied (e.g., show a snackbar or empty state)
            }
        }

        // 3. Launch the request immediately on open
        requestPermissionLauncher.launch(permissionsToRequest)

        enableEdgeToEdge()
        setContent {
            val navController = androidx.navigation.compose.rememberNavController() // From Navigation library
            val systemViewModel: AppSystemViewModel = hiltViewModel()
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val chatGroupListViewModel: ChatGroupListViewModel = hiltViewModel()

            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainView(navController, systemViewModel)
                }
                composable("search_group") {
                    SearchChatGroupView(navController, chatGroupListViewModel)
                }
                navigation(startDestination = "chatview", route = "chat") {
                    composable("chatview/{chatId}",
                        arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { backStackEntry ->
                        val chatViewModel: ChatViewModel = hiltViewModel()

                        ChatView(navController,  systemViewModel, chatViewModel)
                    }
                    composable("chat_group_detail/{chatId}",
                        arguments = listOf(navArgument("chatId") { type = NavType.StringType})
                    ) {  backStackEntry ->
                        val attachmentViewModel: AttachmentViewModel = hiltViewModel()
                        val chatViewModel: ChatViewModel = hiltViewModel()
                        ChatGroupDetailView(navController, attachmentViewModel, chatViewModel)
                    }
                }
                composable("profile_edit") {
                    ProfileEditView(onNavigateBack = { navController.popBackStack() }, profileViewModel)
                }
                composable("settings") {
                    SettingsView(navController, systemViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. The receiver now has access to the initialized manager and channel
        receiver = wifiService.getWifiDirectBroadcastReceiver()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

}
