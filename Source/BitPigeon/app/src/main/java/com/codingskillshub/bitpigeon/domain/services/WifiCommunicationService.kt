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
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class WifiCommunicationService @Inject constructor(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SERVICE_TYPE = "_bitpigeon._tcp"
        private const val SERVICE_NAME = "BitPigeon_Chat_"
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

    private val refreshMutex = Mutex()

    var onServiceAdvertisingChanged: ((Boolean) -> Unit)? = null

    private var user: User? = null

    fun updateWifiStatus(enabled: Boolean) {
        // 3. Updating the value automatically emits a signal to all collectors
        _isWifiEnabled.value = enabled

        if (enabled) {
            if (hasWifiDirectPermissions()) {
//                discoverPeers()
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
                    Log.d("WifiCommService", "Peer Discovery Started Successfully")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WifiCommService", "Peer Discovery Failed: $reason")
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
                if (info.isGroupOwner) {
                    _deviceAddress.value = info.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
                } else {
                    stopServiceAdvertising()
                }
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
        // Check device is available
        if (device.status != WifiP2pDevice.AVAILABLE) {
            Log.e("WifiCommService", "Device ${device.deviceName} not available. Status: ${device.status}")
            return
        }
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

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME+user.id, SERVICE_TYPE, record)

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

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME+user?.id, SERVICE_TYPE, emptyMap())
        manager.removeLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isServiceAdvertising = false
                onServiceAdvertisingChanged?.invoke(isServiceAdvertising)
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
                if (instanceName.startsWith(SERVICE_NAME) && registrationType == SERVICE_TYPE) {
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

    /**
     * Sequential (suspend) version of stopServiceDiscovery to ensure it finishes before starting again.
     */
    @SuppressLint("MissingPermission")
    private suspend fun stopServiceDiscoverySuspend(): Boolean = suspendCancellableCoroutine { cont ->
        if (!isServiceDiscoveryActive) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }

        serviceRequest?.let { request ->
            manager.removeServiceRequest(channel, request, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isServiceDiscoveryActive = false
                    serviceRequest = null
                    _discoveredServices.value = emptyMap()
                    _discoveredUsers.value = emptyMap()
                    Log.d("WifiCommService", "Stop discovery successful (suspend)")
                    if (cont.isActive) cont.resume(true)
                }

                override fun onFailure(reason: Int) {
                    Log.e("WifiCommService", "Stop discovery failed (suspend): $reason")
                    if (cont.isActive) cont.resume(false)
                }
            })
        } ?: run {
            if (cont.isActive) cont.resume(true)
        }
    }

    /**
     * Sequential (suspend) version of startServiceDiscovery.
     */
    @SuppressLint("MissingPermission")
    private suspend fun startServiceDiscoverySuspend(): Boolean = suspendCancellableCoroutine { cont ->
        if (isServiceDiscoveryActive) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }

        // Set up service response listeners
        manager.setDnsSdResponseListeners(channel,
            { instanceName, registrationType, device ->
                if (instanceName == SERVICE_NAME && registrationType == SERVICE_TYPE) {
                    val currentServices = _discoveredServices.value.toMutableMap()
                    currentServices[device.deviceAddress] = device
                    _discoveredServices.value = currentServices
                }
            },
            { fullDomainName, txtRecordMap, device ->
                val userInfoJson = txtRecordMap["userInfo"]
                if (userInfoJson != null) {
                    try {
                        val gson = Gson()
                        val user = gson.fromJson(userInfoJson, User::class.java)
                        val currentUsers = _discoveredUsers.value.toMutableMap()
                        currentUsers[device.deviceAddress] = Pair(user, device)
                        _discoveredUsers.value = currentUsers
                    } catch (e: Exception) {
                        Log.e("WifiCommService", "Failed to parse user info: ${e.message}")
                    }
                }
            }
        )

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        manager.addServiceRequest(channel, serviceRequest!!, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                manager.discoverServices(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        isServiceDiscoveryActive = true
                        Log.d("WifiCommService", "Start discovery successful (suspend)")
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onFailure(reason: Int) {
                        Log.e("WifiCommService", "Discover services failed (suspend): $reason")
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiCommService", "Add service request failed (suspend): $reason")
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    /**
     * Backend refresh logic: Sequential stop then start discovery.
     */
    suspend fun refreshDiscovery() = refreshMutex.withLock {
        if (!hasWifiDirectPermissions()) {
            Log.w("WifiCommService", "Refresh failed: Missing permissions")
            return@withLock
        }

        // 1. Sequentially stop existing discovery and wait for callback
        stopServiceDiscoverySuspend()
        
        // 2. Clear current lists
        _discoveredServices.value = emptyMap()
        _discoveredUsers.value = emptyMap()

        // 3. Sequentially restart discovery and wait for callback
        startServiceDiscoverySuspend()

        // 4. Give discovery some time to populate results
        withTimeoutOrNull(5000) {
            delay(3000) 
        }
        
        Log.d("WifiCommService", "Refresh cycle completed")
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
