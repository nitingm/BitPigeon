package com.codingskillshub.bitpigeon.infrastructure

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import java.net.NetworkInterface
import java.util.Collections
import android.util.Log

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class WifiDirectBroadcastReceiver(
    private val onStateChanged: (Boolean) -> Unit,
    private val onPeersChanged: () -> Unit,
    private val onConnectionChanged: (NetworkInfo?) -> Unit,
    private val onDeviceChanged: (WifiP2pDevice) -> Unit
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // 1. Check if Wi-Fi P2P is enabled or disabled
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                onStateChanged(isEnabled)
                Log.d("WifiDirectBR", "P2P State Enabled: $isEnabled")
            }

            // 2. The list of available peers has changed
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d("WifiDirectBR", "Peers changed")
                onPeersChanged()
            }

            // 3. The state of the Wi-Fi P2P connection has changed
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                Log.d("WifiDirectBR", "Connection changed: Connected=${networkInfo?.isConnected}")
                onConnectionChanged(networkInfo)
            }

            // 4. This device's configuration details have changed
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Log.d("WifiDirectBR", "This device changed")
                val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                device?.let {
                    onDeviceChanged(it)
                }
            }

            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                Log.d("WifiDirectBR", "Discovery changed")
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)

                when (state) {
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> {
                        Log.d("WifiDirectBR", "Discovery Started")
                        // You could trigger a UI loading spinner here
                    }
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> {
                        Log.d("WifiDirectBR", "Discovery Stopped")
                        // You could hide the loading spinner here
                    }
                }
            }
        }
    }
}
