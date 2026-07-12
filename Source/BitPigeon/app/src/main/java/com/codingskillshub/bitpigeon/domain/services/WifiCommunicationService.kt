package com.codingskillshub.bitpigeon.domain.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
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
import com.codingskillshub.bitpigeon.domain.types.WifiDirectPeer
import com.codingskillshub.bitpigeon.infrastructure.WifiDirectBroadcastReceiver
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // Keep a reference to the local serviceInfo we registered so removal can use the same object
    private var localServiceInfo: WifiP2pDnsSdServiceInfo? = null

    // 1. Use MutableStateFlow to hold the state
    private val _isWifiEnabled = MutableStateFlow(false)
    // 2. Expose as read-only StateFlow for ViewModels to collect
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _isWifiDirectServiceAdvertisingEnabled = MutableStateFlow(false)
    val isWifiDirectServiceAdvertisingEnabled: StateFlow<Boolean> = _isWifiDirectServiceAdvertisingEnabled.asStateFlow()

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

    private val _discoveredPeers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())
    val discoveredPeers = _discoveredPeers.asStateFlow()

    private var serviceRequest: WifiP2pDnsSdServiceRequest? = null
    private var isServiceAdvertising = false
    private var isServiceDiscoveryActive = false

    private var localDeviceInfo: WifiP2pDevice? = null

    private val refreshMutex = Mutex()

    var onServiceAdvertisingChanged: ((Boolean) -> Unit)? = null

    private var user: User? = null

    init {
        disconnectFromAllDevices()
    }

    fun setUserDetails(user: User) {
        this.user = user
    }

    fun getLocalDeviceInfo(): WifiP2pDevice? {
        return localDeviceInfo
    }

    fun getDiscoveredPeers(): List<WifiP2pDevice> {
        return _peers.value
    }

    /**
     * Robust version: Switch between service advertising and discovery with proper sequencing.
     * When enabled: stops discovery first, then starts advertising.
     * When disabled: stops advertising first, then starts discovery.
     * This is a fire-and-forget coroutine function called from UI.
     *
     * Note: This function launches in GlobalScope. Consider injecting a CoroutineScope
     * for better lifecycle management in production code.
     */
    fun switchWifiDirectServiceAdvertising(enabled: Boolean) {
        _isWifiDirectServiceAdvertisingEnabled.value = enabled
        GlobalScope.launch {
            try {
                switchWifiDirectServiceAdvertisingSuspend(enabled)
            } catch (e: Exception) {
                Log.e("WifiCommService", "Exception in switchWifiDirectServiceAdvertising: ${e.message}")
            }
        }
    }

    /**
     * Suspend version: Sequential switching between service advertising and discovery.
     * When enabled: stops discovery FIRST, waits for completion, then starts advertising.
     * When disabled: stops advertising FIRST, waits for completion, then starts discovery.
     */
    @SuppressLint("MissingPermission")
    private suspend fun switchWifiDirectServiceAdvertisingSuspend(enabled: Boolean) {
        if (enabled) {
            Log.d("WifiCommService", "Enabling service advertising: stopping discovery first...")
            // 1. Stop discovery first and wait for it to complete
            stopServiceDiscoverySuspend()
            Log.d("WifiCommService", "Discovery stopped. Now starting service advertising...")
            // 2. Then start advertising
            startServiceAdvertisingSuspend(user ?: User("", "", "", "", "", ""))
            Log.d("WifiCommService", "Service advertising started")
        } else {
            Log.d("WifiCommService", "Disabling service advertising: stopping advertising first...")
            // 1. Stop advertising first and wait for it to complete
            stopServiceAdvertisingSuspend()
            Log.d("WifiCommService", "Advertising stopped. Now starting service discovery...")
            // 2. Then start discovery
            startServiceDiscoverySuspend()
            Log.d("WifiCommService", "Service discovery started")
        }
    }

    private fun updateWifiStatus(enabled: Boolean) {
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
            disconnectFromAllDevices()
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
                Log.d("WifiCommService", "Peers: ${Gson().toJson(refreshedPeers)}")
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

    /**
     * Start advertising BitPigeon service to other devices (async wrapper)
     */
    @SuppressLint("MissingPermission")
    fun startServiceAdvertising(user: User, isServer: Boolean = false) {
        GlobalScope.launch {
            try {
                startServiceAdvertisingSuspend(user, isServer)
            } catch (e: Exception) {
                Log.e("WifiCommService", "Exception in startServiceAdvertising: ${e.message}")
            }
        }
    }

    /**
     * Sequential (suspend) version of startServiceAdvertising to ensure it finishes before other operations.
     */
    @SuppressLint("MissingPermission")
    private suspend fun startServiceAdvertisingSuspend(user: User, isServer: Boolean = false): Boolean = suspendCancellableCoroutine { cont ->
        if (!hasWifiDirectPermissions()) {
            Log.w("WifiCommService", "Start advertising failed: Missing permissions")
            cont.resume(false)
            return@suspendCancellableCoroutine
        }

        if (isServiceAdvertising) {
            Log.d("WifiCommService", "Service advertising already active, resuming immediately")
            cont.resume(true)
            return@suspendCancellableCoroutine
        }

        this.user = user

        val record = mapOf(
            "serviceName" to SERVICE_NAME,
            "version" to "1.0",
            "deviceName" to (_deviceName.value.takeIf { it != "Unknown Device" } ?: "BitPigeon User"),
            "userId" to user.id,
            "userName" to user.name,
            "isServer" to isServer.toString()
        ).toSafeTxtRecord()

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME + user.id, SERVICE_TYPE, record)
        // keep reference so removeLocalService gets the same object instance
        localServiceInfo = serviceInfo

        manager.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isServiceAdvertising = true
                _isWifiDirectServiceAdvertisingEnabled.value = true
                onServiceAdvertisingChanged?.invoke(isServiceAdvertising)
                Log.d("WifiCommService", "Start advertising successful (suspend)")
                if (cont.isActive) cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiCommService", "Start advertising failed (suspend): $reason")
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    /**
     * Stop advertising BitPigeon service (async wrapper)
     */
    @SuppressLint("MissingPermission")
    fun stopServiceAdvertising() {
        GlobalScope.launch {
            try {
                stopServiceAdvertisingSuspend()
            } catch (e: Exception) {
                Log.e("WifiCommService", "Exception in stopServiceAdvertising: ${e.message}")
            }
        }
    }

    /**
     * Sequential (suspend) version of stopServiceAdvertising to ensure it finishes before starting again.
     */
    @SuppressLint("MissingPermission")
    private suspend fun stopServiceAdvertisingSuspend(): Boolean = suspendCancellableCoroutine { cont ->
        if (!isServiceAdvertising) {
            Log.d("WifiCommService", "Service advertising not active, resuming immediately")
            cont.resume(true)
            return@suspendCancellableCoroutine
        }

        val serviceInfo = localServiceInfo ?: WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME + (user?.id ?: ""), SERVICE_TYPE, emptyMap())
        manager.removeLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isServiceAdvertising = false
                localServiceInfo = null
                _isWifiDirectServiceAdvertisingEnabled.value = false
                onServiceAdvertisingChanged?.invoke(isServiceAdvertising)
                Log.d("WifiCommService", "Stop advertising successful (suspend)")
                if (cont.isActive) cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiCommService", "Stop advertising failed (suspend): $reason. Trying clearLocalServices fallback.")
                // Fallback: clear all local services
                manager.clearLocalServices(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        isServiceAdvertising = false
                        localServiceInfo = null
                        _isWifiDirectServiceAdvertisingEnabled.value = false
                        onServiceAdvertisingChanged?.invoke(isServiceAdvertising)
                        Log.d("WifiCommService", "Cleared local services (fallback, suspend)")
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onFailure(reason: Int) {
                        Log.e("WifiCommService", "Failed to clear local services fallback (suspend): $reason")
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }
        })
    }

    /**
     * Start discovering BitPigeon services on other devices (async wrapper)
     */
    @SuppressLint("MissingPermission")
    fun startServiceDiscovery() {
        GlobalScope.launch {
            try {
                startServiceDiscoverySuspend()
            } catch (e: Exception) {
                Log.e("WifiCommService", "Exception in startServiceDiscovery: ${e.message}")
            }
        }
    }

    /**
     * Stop service discovery (async wrapper)
     */
    @SuppressLint("MissingPermission")
    fun stopServiceDiscovery() {
        GlobalScope.launch {
            try {
                stopServiceDiscoverySuspend()
            } catch (e: Exception) {
                Log.e("WifiCommService", "Exception in stopServiceDiscovery: ${e.message}")
            }
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
                    _discoveredPeers.value = emptyList()
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
                val regTypeMatches = registrationType.contains(SERVICE_TYPE) || registrationType.endsWith(SERVICE_TYPE)
                if (instanceName.startsWith(SERVICE_NAME) && regTypeMatches) {
                    val currentServices = _discoveredServices.value.toMutableMap()
                    currentServices[device.deviceAddress] = device
                    _discoveredServices.value = currentServices
                }
            },
            { fullDomainName, txtRecordMap, device ->
                val userId = txtRecordMap["userId"]
                if (userId != null) {
                    try {
                        addDiscoveredPeer(WifiDirectPeer(
                            deviceName = device.deviceName,
                            deviceMacAddress = device.deviceAddress,
                            isGroupOwner = device.isGroupOwner,
                            userId = userId,
                            userName = txtRecordMap["userName"] ?: ""
                        ))
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
        discoverPeers()
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
        _discoveredPeers.value = emptyList()

        // 3. Sequentially restart discovery and wait for callback
        startServiceDiscoverySuspend()

        // 4. Give discovery some time to populate results
        withTimeoutOrNull(5000) {
            delay(3000) 
        }
        
        Log.d("WifiCommService", "Refresh cycle completed")
    }

    @SuppressLint("MissingPermission")
    fun leaveGroup() {
        if (hasWifiDirectPermissions()) {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WifiCommService", "Successfully left the group (group removed/left)")
                    _connectionInfo.value = null
                }

                override fun onFailure(reason: Int) {
                    // Reason 2 is BUSY, often happens if already disconnected or no group exists
                    if (reason == WifiP2pManager.BUSY) {
                        Log.d("WifiCommService", "removeGroup failed with BUSY - possibly already disconnected.")
                    } else {
                        Log.e("WifiCommService", "Failed to leave group: $reason")
                    }
                }
            })
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun disconnectFromAllDevices() {
        if (hasWifiDirectPermissions()) {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WifiCommService", "Successfully disconnected from all devices (group removed)")
                    _connectionInfo.value = null
                }

                override fun onFailure(reason: Int) {
                    // Reason 2 is BUSY, often happens if already disconnected or no group exists
                    if (reason == WifiP2pManager.BUSY) {
                        Log.d("WifiCommService", "removeGroup failed with BUSY - possibly already disconnected.")
                    } else {
                        Log.e("WifiCommService", "Failed to disconnect from devices: $reason")
                    }
                }
            })
        }
    }

    private fun Map<String, String>.toSafeTxtRecord(): Map<String, String> {
        return this.mapValues { (key, value) ->
            val keyBytes = key.toByteArray(Charsets.UTF_8)
            // RFC 6763: Each TXT record string (key=value) is limited to 255 bytes.
            // overhead: 1 byte for '='.
            val maxAvailableValueBytes = 255 - keyBytes.size - 1

            if (maxAvailableValueBytes <= 0) return@mapValues ""

            val valueBytes = value.toByteArray(Charsets.UTF_8)
            if (valueBytes.size > maxAvailableValueBytes) {
                var truncatedValue = value
                while (truncatedValue.toByteArray(Charsets.UTF_8).size > maxAvailableValueBytes && truncatedValue.isNotEmpty()) {
                    truncatedValue = truncatedValue.dropLast(1)
                }
                truncatedValue
            } else {
                value
            }
        }
    }

    private fun hasWifiDirectPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, NEARBY_WIFI_DEVICES is sufficient if neverForLocation is used
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older versions, location permission is required
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            hasFine || hasCoarse
        }
    }

    fun getWifiDirectBroadcastReceiver(): BroadcastReceiver {
        return WifiDirectBroadcastReceiver(
            onStateChanged = { isEnabled ->
                /* Handle Wi-Fi P2P toggle state */
                updateWifiStatus(isEnabled)
            },
            onPeersChanged = {
                requestPeers()
            },
            onConnectionChanged = { networkInfo ->
                /* Handle connection/disconnection logic */
                updateNetworkInfo(networkInfo)
            },
            onDeviceChanged = { device ->
                /* Update local device info */
                localDeviceInfo = device
                device.let {
                    _deviceName.value = it.deviceName
                    Log.d("WifiCommService", "Local device updated: ${it.deviceName} (${it.deviceAddress})")
                }
            }
        )
    }

    private fun addDiscoveredPeer(discoveredPeer: WifiDirectPeer) {
        val currentPeers = _discoveredPeers.value.toMutableList()

        if (!currentPeers.contains(discoveredPeer)) {
            currentPeers.add(discoveredPeer)
            _discoveredPeers.value = currentPeers
        }
    }
}
