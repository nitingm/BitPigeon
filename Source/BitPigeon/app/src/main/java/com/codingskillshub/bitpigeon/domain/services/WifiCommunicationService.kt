package com.codingskillshub.bitpigeon.domain.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.codingskillshub.bitpigeon.domain.entities.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiCommunicationService @Inject constructor(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SERVICE_TYPE = "_bitpigeon._tcp"
        private const val SERVICE_NAME = "BitPigeon Chat"
    }

    // 1. Use MutableStateFlow to hold the state
    private val _isWifiEnabled = MutableStateFlow(false)
    // 2. Expose as read-only StateFlow for ViewModels to collect
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    // 2. State for Discovered Peers
    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peersList: StateFlow<List<WifiP2pDevice>> = _peers.asStateFlow()

    // 3. State for Connection Info (IP addresses, Group Owner status)
    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _deviceName = MutableStateFlow("Unknown Device")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _deviceAddress = MutableStateFlow("Unknown Device")
    val deviceAddress: StateFlow<String> = _deviceAddress.asStateFlow()

    // Service Discovery State
    private val _discoveredServices = MutableStateFlow<Map<String, WifiP2pDevice>>(emptyMap())
    val discoveredServices: StateFlow<Map<String, WifiP2pDevice>> = _discoveredServices.asStateFlow()

    // Discovered Users with their device info
    private val _discoveredUsers = MutableStateFlow<Map<String, Pair<User, WifiP2pDevice>>>(emptyMap())
    val discoveredUsers: StateFlow<Map<String, Pair<User, WifiP2pDevice>>> = _discoveredUsers.asStateFlow()

    private var serviceRequest: WifiP2pDnsSdServiceRequest? = null
    private var isServiceAdvertising = false
    private var isServiceDiscoveryActive = false

    fun updateWifiStatus(enabled: Boolean) {
        // 3. Updating the value automatically emits a signal to all collectors
        _isWifiEnabled.value = enabled

        if (enabled) {
            if (hasWifiDirectPermissions()) {
                discoverPeers()
                startServiceDiscovery()
            } else {
                Log.w("WifiCommService", "Skipping discovery: Permissions not yet granted.")
            }
        } else {
            // Handle disabling
            _peers.value = emptyList()
            stopServiceAdvertising()
            stopServiceDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        if (hasWifiDirectPermissions()) {
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
            if (_connectionInfo.value != null) {
                // Already connected to a group, remove it first
                manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d("WifiCommService", "Existing group removed, now connecting to ${device.deviceName}")
                        doConnect(device)
                    }

                    override fun onFailure(reason: Int) {
                        Log.e("WifiCommService", "Failed to remove existing group: $reason")
                    }
                })
            } else {
                doConnect(device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun doConnect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
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

    // Service Discovery Methods

    /**
     * Start advertising BitPigeon service to other devices
     */
    @SuppressLint("MissingPermission")
    fun startServiceAdvertising(user: User, isServer: Boolean = false) {
        if (!hasWifiDirectPermissions() || isServiceAdvertising) return

        val gson = Gson()
        val userJson = gson.toJson(user)

        val record = mapOf(
            "serviceName" to SERVICE_NAME,
            "version" to "1.0",
            "deviceName" to (_deviceName.value.takeIf { it != "Unknown Device" } ?: "BitPigeon User"),
            "userInfo" to userJson,
            "isServer" to isServer.toString()
        )

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, SERVICE_TYPE, record)

        manager.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isServiceAdvertising = true
                Log.d("WifiCommService", "Service advertising started successfully")
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiCommService", "Service advertising failed: $reason")
            }
        })
    }

    /**
     * Stop advertising BitPigeon service
     */
    @SuppressLint("MissingPermission")
    fun stopServiceAdvertising() {
        if (!isServiceAdvertising) return

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, SERVICE_TYPE, emptyMap())
        manager.removeLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isServiceAdvertising = false
                Log.d("WifiCommService", "Service advertising stopped successfully")
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiCommService", "Service advertising stop failed: $reason")
            }
        })
    }

    /**
     * Start discovering BitPigeon services on other devices
     */
    @SuppressLint("MissingPermission")
    fun startServiceDiscovery() {
        if (!hasWifiDirectPermissions() || isServiceDiscoveryActive) return

        // Set up service response listeners
        manager.setDnsSdResponseListeners(channel,
            { instanceName, registrationType, device ->
                // Called when a service is found
                Log.d("WifiCommService", "Service found: $instanceName on ${device.deviceName}")
                if (instanceName == SERVICE_NAME && registrationType == SERVICE_TYPE) {
                    val currentServices = _discoveredServices.value.toMutableMap()
                    currentServices[device.deviceAddress] = device
                    _discoveredServices.value = currentServices
                }
            },
            { fullDomainName, txtRecordMap, device ->
                // Called when TXT record is available
                Log.d("WifiCommService", "TXT record for ${device.deviceName}: $txtRecordMap")

                // Parse user info from TXT record
                val userInfoJson = txtRecordMap["userInfo"]
                if (userInfoJson != null) {
                    try {
                        val gson = Gson()
                        val user = gson.fromJson(userInfoJson, User::class.java)
                        val currentUsers = _discoveredUsers.value.toMutableMap()
                        currentUsers[device.deviceAddress] = Pair(user, device)
                        _discoveredUsers.value = currentUsers
                        Log.d("WifiCommService", "Parsed user info for ${user.name}")
                    } catch (e: Exception) {
                        Log.e("WifiCommService", "Failed to parse user info: ${e.message}")
                    }
                }
            }
        )

        // Create and add service discovery request
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        manager.addServiceRequest(channel, serviceRequest!!, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiCommService", "Service request added successfully")
                // Now start discovery
                manager.discoverServices(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        isServiceDiscoveryActive = true
                        Log.d("WifiCommService", "Service discovery started successfully")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e("WifiCommService", "Service discovery failed: $reason")
                    }
                })
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiCommService", "Service request addition failed: $reason")
            }
        })
    }

    /**
     * Stop service discovery
     */
    @SuppressLint("MissingPermission")
    fun stopServiceDiscovery() {
        if (!isServiceDiscoveryActive) return

        serviceRequest?.let { request ->
            manager.removeServiceRequest(channel, request, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isServiceDiscoveryActive = false
                    serviceRequest = null
                    _discoveredServices.value = emptyMap()
                    _discoveredUsers.value = emptyMap()
                    Log.d("WifiCommService", "Service discovery stopped successfully")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WifiCommService", "Service discovery stop failed: $reason")
                }
            })
        }
    }

    private fun hasWifiDirectPermissions(): Boolean {
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasNearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return hasLocation && hasNearby
    }
}
