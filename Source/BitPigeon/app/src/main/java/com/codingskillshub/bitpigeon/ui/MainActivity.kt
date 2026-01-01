package com.codingskillshub.bitpigeon.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation

import com.codingskillshub.bitpigeon.infrastructure.WifiDirectBroadcastReceiver
import com.codingskillshub.bitpigeon.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import dagger.hilt.android.AndroidEntryPoint
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

    private val systemViewModel: AppSystemViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[AppSystemViewModel::class.java]
    }

    // Inside your Activity
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    // Define the permissions needed based on Android Version
    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
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
            } else {
                // Handle permission denied (e.g., show a snackbar or empty state)
            }
        }

        // 3. Launch the request immediately on open
        requestPermissionLauncher.launch(permissionsToRequest)

        enableEdgeToEdge()
        setContent {
            val navController = androidx.navigation.compose.rememberNavController() // From Navigation library

            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainView(navController, systemViewModel)
                }
                navigation(startDestination = "chatview", route = "chat") {
                    composable("chatview/{chatId}") { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getString("chatId")
                        val dummyMessages = listOf(
                            MessageData("1", "Aman Gupta", "Hey! Is the Wi-Fi P2P working?", "10:00", false),
                            MessageData("2", "Me", "Yes, just finished the ChatView implementation.", "10:01", true),
                            MessageData("3", "Aman Gupta", "Awesome, try sending an emoji! 🚀", "10:02", false),
                            MessageData("4", "Me", "Working perfectly fine. 👍", "10:03", true),
                        )

                        ChatView(navController, systemViewModel, chatId.toString(), dummyMessages)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. The receiver now has access to the initialized manager and channel
        receiver = WifiDirectBroadcastReceiver(
            onStateChanged = { isEnabled ->
                /* Handle Wi-Fi P2P toggle state */
                wifiService.updateWifiStatus(isEnabled)
            },
            onPeersChanged = {
                wifiService.requestPeers()
            },
            onConnectionChanged = { networkInfo ->
                /* Handle connection/disconnection logic */
                wifiService.updateNetworkInfo(networkInfo)
            },
            onDeviceChanged = {
                /* Update local device info */

            }
        )
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

}