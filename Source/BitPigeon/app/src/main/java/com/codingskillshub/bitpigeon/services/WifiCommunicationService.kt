package com.codingskillshub.bitpigeon.services

import android.Manifest
import android.annotation.SuppressLint
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiCommunicationService @Inject constructor(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    // 1. Use MutableStateFlow to hold the state
    private val _isWifiEnabled = MutableStateFlow(false)
    // 2. Expose as read-only StateFlow for ViewModels to collect
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    // 2. State for Discovered Peers
    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers.asStateFlow()

    // 3. State for Connection Info (IP addresses, Group Owner status)
    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _deviceName = MutableStateFlow("Unknown Device")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()


    fun updateWifiStatus(enabled: Boolean) {
        // 3. Updating the value automatically emits a signal to all collectors
        _isWifiEnabled.value = enabled

        if (enabled) {
            if (hasWifiDirectPermissions()) {
                discoverPeers()
            } else {
                Log.w("WifiCommService", "Skipping discovery: Permissions not yet granted.")
            }
        } else {
            // Handle disabling
            _peers.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        if (!hasWifiDirectPermissions()) {
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WifiCommService", "Discovery Started Successfully")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WifiCommService", "Discovery Failed: $reason")
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        if (hasWifiDirectPermissions()) {
            manager.requestPeers(channel) { peerList: WifiP2pDeviceList? ->
                val refreshedPeers = peerList?.deviceList?.toList() ?: emptyList()
                _peers.value = refreshedPeers
                Log.d("WifiCommService", "Found ${refreshedPeers.size} peers")
            }
        }
    }

    /**
     * Called by BroadcastReceiver when WIFI_P2P_CONNECTION_CHANGED_ACTION triggers
     */
    fun updateNetworkInfo(networkInfo: NetworkInfo?) {
        if (networkInfo?.isConnected ?: false) {
            manager.requestConnectionInfo(channel) { info ->
                _connectionInfo.value = info
                Log.d("WifiCommService", "Connected. Group Owner: ${info.isGroupOwner}, IP: ${info.groupOwnerAddress?.hostAddress}")
            }
        } else {
            _connectionInfo.value = null
            Log.d("WifiCommService", "Disconnected from P2P group")
        }
    }

    fun setDeviceName(name: String) {
        _deviceName.value = name
    }

    /**
     * Call this when a user clicks on a device in the UI list
     */
    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {
        if (hasWifiDirectPermissions()) {
            val config = android.net.wifi.p2p.WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
            }

            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(
                        "WifiCommService",
                        "Connection initiation successful with ${device.deviceName}"
                    )
                }

                override fun onFailure(reason: Int) {
                    Log.e("WifiCommService", "Connection failed: $reason")
                }
            })
        }
    }

    private fun hasWifiDirectPermissions(): Boolean {
        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasNearby = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        return hasLocation && hasNearby
    }

}
