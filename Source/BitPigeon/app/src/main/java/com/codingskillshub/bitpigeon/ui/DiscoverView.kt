package com.codingskillshub.bitpigeon.ui

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.*
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.R
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.types.WifiDirectPeer
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredGroupEntry
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredPeerEntry
import com.codingskillshub.bitpigeon.ui.theme.AppTheme
import com.codingskillshub.bitpigeon.ui.viewmodels.DiscoverViewModel
import com.codingskillshub.bitpigeon.ui.composables.QRCodePopUp
import com.codingskillshub.bitpigeon.ui.composables.QRCodeScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverView(
    navController: NavController,
    discoverViewModel: DiscoverViewModel = viewModel()
) {
    val discoveredPeers by discoverViewModel.nearbyPeers.collectAsState()
    val availablePeerClients by discoverViewModel.availableClients.collectAsState()
    val isRefreshing by discoverViewModel.isRefreshing.collectAsState()
    val isWifiDirectServiceAdvertisingEnabled by discoverViewModel.isWifiDirectServiceAdvertisingEnabled.collectAsState()
    val isWifiEnabled by discoverViewModel.isWifiEnabled.collectAsState()
    val groupOwnerName by discoverViewModel.groupOwnerName.collectAsState()
    val isGroupOwner by discoverViewModel.isGroupOwner.collectAsState()
    val isConnectedToGroup by discoverViewModel.isConnectedToGroup.collectAsState()
    val showQrPopup by discoverViewModel.showQrPopup.collectAsState()
    val showScanner by discoverViewModel.showScanner.collectAsState()
    val qrPayloadText by discoverViewModel.qrPayloadText.collectAsState()
    val isQrValid by discoverViewModel.isQrValid.collectAsState()
    val wifiDirectPeersCount by discoverViewModel.wifiDirectPeersCount.collectAsState()
    val isPeerDiscoveryActive by discoverViewModel.isPeerDiscoveryActive.collectAsState()

    discoverViewModel.onChatGroupInvoked = { groupId ->
        navController.navigate("chatview/$groupId")
    }

    DiscoverViewContent(
        isWifiDirectServiceAdvertisingEnabled,
        discoveredPeers,
        groupOwnerName = groupOwnerName,
        isGroupOwner = isGroupOwner,
        availablePeerClients,
        isRefreshing,
        isWifiEnabled,
        isPeerDiscoveryActive = isPeerDiscoveryActive,
        wifiDirectPeersCount = wifiDirectPeersCount,
        isConnectedToGroup = isConnectedToGroup,
        onRefresh = { discoverViewModel.refresh() },
        onConnectToPeer = { peer -> discoverViewModel.connectToPeer(peer) },
        onOpenDirectChat = { user -> discoverViewModel.createAndOpenDirectChat(user) },
        onSwitchAdvertising = { enabled -> discoverViewModel.switchAdvertising(enabled)},
        onShowQr = { discoverViewModel.showQrPopup() },
        onScanQr = { discoverViewModel.openScanner() },
        onGroupExit = { discoverViewModel.exitGroup() }
    )

    if (showQrPopup) {
        QRCodePopUp(
            payloadText = qrPayloadText,
            isQrValid = isQrValid,
            onDismiss = { discoverViewModel.hideQrPopup() }
        )
    }

    if (showScanner) {
        QRCodeScannerView(
            onScanResult = { discoverViewModel.handleScanResult(it) },
            onCancel = { discoverViewModel.hideScanner() }
        )
    }
}

@Composable
fun DiscoverViewContent(
    isWifiDirectServiceAdvertisingEnabled: Boolean = false,
    usersList: List<WifiDirectPeer> = emptyList(),
    groupOwnerName: String = "",
    isGroupOwner: Boolean = false,
    availablePeerClients: List<Client> = emptyList(),
    isRefreshing: Boolean = false,
    isWifiEnabled: Boolean = true,
    isPeerDiscoveryActive: Boolean = false,
    isConnectedToGroup: Boolean = false,
    wifiDirectPeersCount: Int = 0,
    onRefresh: () -> Unit = {},
    onConnectToPeer: (WifiDirectPeer) -> Unit = {},
    onOpenDirectChat: (User) -> Unit = {},
    onSwitchAdvertising: (Boolean) -> Unit = {},
    onShowQr: () -> Unit = {},
    onScanQr: () -> Unit = {},
    onGroupExit: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
               Text(
                   text = "Advertise Nearby",
                   style = MaterialTheme.typography.bodyLarge.copy(
                       fontWeight = FontWeight.Medium,
                       fontSize = 15.sp
                   ),
                   modifier = Modifier
                       .weight(1f)
               )
                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                Switch(
                    checked = isWifiDirectServiceAdvertisingEnabled,
                    onCheckedChange = { onSwitchAdvertising(!isWifiDirectServiceAdvertisingEnabled) },
                    enabled = isWifiEnabled && ((isConnectedToGroup && isGroupOwner) || !isConnectedToGroup),
                    modifier = Modifier
                        .scale(0.8f) 
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (availablePeerClients.isNotEmpty()) {
                    item {
                        DiscoveredGroupEntry(
                            groupName = groupOwnerName,
                            availablePeerClients,
                            onClick = { user ->
                                onOpenDirectChat(user)
                            },
                            onGroupExit = {
                                onGroupExit()
                            }
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 0.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Discovered Users",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isPeerDiscoveryActive) {
                                val infiniteTransition = rememberInfiniteTransition(label = "radar")

                                // Animates the scale of the outer ring from 0% to 250%
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 2.5f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "scale"
                                )

                                // Animates the transparency so it fades out as it expands
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.6f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "alpha"
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(contentAlignment = Alignment.Center) {
                                    // The pulsating ripple/wave
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                this.alpha = alpha
                                            }
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )

                                    // The static central Radar icon
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = "Searching",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isWifiEnabled) {
                    if (availablePeerClients.isEmpty() && usersList.isEmpty()) {
                        item {
                            EmptyState("No users found nearby.\nPull down to scan again.")
                        }
                    } else {
                        // 'items' handles the recycling and lazy loading automatically
                        items(
                            items = usersList,
                            // Providing a 'key' helps Compose optimize list updates/reordering
                            key = { device -> device.deviceMacAddress }
                        ) { device ->
                            DiscoveredPeerEntry(
                                name = device.userName,
                                statusString = "(${device.deviceName})",
                                onClick = { 
                                    if (!isConnectedToGroup) {
                                        onConnectToPeer(device)
                                    } else {
                                        Toast.makeText(context, "Cannot connect to a new user. You are already part of a group", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    item {
                        WifiDisabledState("Wi-Fi is not enabled. \n Turn wifi ON???")
                    }
                }


            }
        }
        Text(
            text = "Found $wifiDirectPeersCount WiFi devices nearby.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(16.dp, 8.dp, 16.dp, 0.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShowQr,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Show QR")
            }
            Button(
                onClick = onScanQr,
                modifier = Modifier.weight(1f),
                enabled = !isConnectedToGroup
            ) {
                Text(text = "Scan QR")
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.pigeon_discover),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun WifiDisabledState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.pigeon_wifi_broken),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun DiscoverViewPreview() {
    val samplePeers = listOf(
        WifiDirectPeer(deviceName = "Aman's Phone", deviceMacAddress = "00:00:00:00:00:01", userName = "Aman Gupta", userId = "1"),
        WifiDirectPeer(deviceName = "John's Phone", deviceMacAddress = "00:00:00:00:00:02", userName = "John Doe", userId = "2"),
        WifiDirectPeer(deviceName = "Project Group", deviceMacAddress = "00:00:00:00:00:03", userName = "Project Group", userId = "3"),
        WifiDirectPeer(deviceName = "Mama's Phone", deviceMacAddress = "00:00:00:00:00:04", userName = "Mama", userId = "4"),
        WifiDirectPeer(deviceName = "Support Device", deviceMacAddress = "00:00:00:00:00:05", userName = "BitPigeon Support", userId = "5")
    )

    DiscoverViewContent(true, samplePeers, isConnectedToGroup = false, isPeerDiscoveryActive = true)
}

@Preview(showBackground = true)
@Composable
fun DiscoverViewEmptyStatePreview() {
    DiscoverViewContent(false, emptyList(), isConnectedToGroup = false, isPeerDiscoveryActive = false)
}


@Preview(showBackground = true)
@Composable
fun DiscoverViewWifiDisabledStatePreview() {
    DiscoverViewContent(false, isWifiEnabled = false, usersList = emptyList())
}

@Preview(showBackground = true)
@Composable
fun DiscoverViewDarkPreview() {
    val samplePeers = listOf(
        WifiDirectPeer(deviceName = "Aman's Phone", deviceMacAddress = "00:00:00:00:00:01", userName = "Aman Gupta", userId = "1"),
        WifiDirectPeer(deviceName = "John's Phone", deviceMacAddress = "00:00:00:00:00:02", userName = "John Doe", userId = "2"),
        WifiDirectPeer(deviceName = "Project Group", deviceMacAddress = "00:00:00:00:00:03", userName = "Project Group", userId = "3"),
        WifiDirectPeer(deviceName = "Mama's Phone", deviceMacAddress = "00:00:00:00:00:04", userName = "Mama", userId = "4"),
        WifiDirectPeer(deviceName = "Support Device", deviceMacAddress = "00:00:00:00:00:05", userName = "BitPigeon Support", userId = "5")
    )
    AppTheme("DARK") {
        DiscoverViewContent(true, samplePeers)
    }
}
