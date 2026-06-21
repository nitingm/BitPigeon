package com.codingskillshub.bitpigeon.ui

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredGroupEntry
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredPeerEntry
import com.codingskillshub.bitpigeon.ui.viewmodels.DiscoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverView(
    navController: NavController,
    discoverViewModel: DiscoverViewModel = viewModel()
) {
    val discoveredUsers by discoverViewModel.discoveredUsers.collectAsState()
    val usersList = discoveredUsers.values.toList()
    val availablePeerClients by discoverViewModel.availableClients.collectAsState()
    val isRefreshing by discoverViewModel.isRefreshing.collectAsState()
    val isWifiDirectServiceAdvertisingEnabled by discoverViewModel.isWifiDirectServiceAdvertisingEnabled.collectAsState()

    discoverViewModel.onChatGroupInvoked = { groupId ->
        navController.navigate("chatview/$groupId")
    }

    DiscoverViewContent(
        isWifiDirectServiceAdvertisingEnabled,
        usersList,
        availablePeerClients,
        isRefreshing,
        onRefresh = { discoverViewModel.refresh() },
        onConnectToPeer = { peer -> discoverViewModel.connectToPeer(peer) },
        onOpenDirectChat = { user -> discoverViewModel.createAndOpenDirectChat(user) },
        onSwitchAdvertising = { enabled -> discoverViewModel.switchAdvertising(enabled)}
    )
}

@Composable
fun DiscoverViewContent(
    isWifiDirectServiceAdvertisingEnabled: Boolean = false,
    usersList: List<Pair<User, WifiP2pDevice>>,
    availablePeerClients: List<Client> = emptyList(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onConnectToPeer: (WifiP2pDevice) -> Unit = {},
    onOpenDirectChat: (User) -> Unit = {},
    onSwitchAdvertising: (Boolean) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center)
                    .fillMaxWidth(),
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
                Switch(
                    checked = isWifiDirectServiceAdvertisingEnabled,
                    onCheckedChange = { onSwitchAdvertising(!isWifiDirectServiceAdvertisingEnabled) }
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
                // Adds spacing at the top and bottom of the list
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (availablePeerClients.isNotEmpty()) {
                    item {
                        DiscoveredGroupEntry(
                            availablePeerClients,
                            onClick = { user ->
                                onOpenDirectChat(user)
                            }
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .background(color = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "Discovered Users",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                        )
                    }
                }

                if (availablePeerClients.isEmpty() && usersList.isEmpty()) {
                    item {
                        EmptyState("No devices found???")
                    }
                } else {
                    // 'items' handles the recycling and lazy loading automatically
                    items(
                        items = usersList,
                        // Providing a 'key' helps Compose optimize list updates/reordering
                        key = { (_, device) -> device.deviceAddress }
                    ) { (user, device) ->
                        DiscoveredPeerEntry(
                            name = user.name,
                            statusString = "${user.email} (${device.deviceName})",
                            onClick = { onConnectToPeer(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Preview(showBackground = true)
@Composable
fun DiscoverViewPreview() {
    // Each WifiP2pDevice needs a unique deviceAddress to avoid duplicate keys in LazyColumn
    val sampleUsers = listOf(
        Pair(User("1", "Aman Gupta", "none", "none", "none", "none"), WifiP2pDevice().apply { deviceAddress = "00:00:00:00:00:01"; deviceName = "Aman's Phone" }),
        Pair(User("2", "John Doe", "none", "none", "none", "none"), WifiP2pDevice().apply { deviceAddress = "00:00:00:00:00:02"; deviceName = "John's Phone" }),
        Pair(User("3", "Project Group", "none", "none", "none", "none"), WifiP2pDevice().apply { deviceAddress = "00:00:00:00:00:03"; deviceName = "Project Group" }),
        Pair(User("4", "Mama", "none", "none", "none", "none"), WifiP2pDevice().apply { deviceAddress = "00:00:00:00:00:04"; deviceName = "Mama's Phone" }),
        Pair(User("5", "BitPigeon Support", "none", "none", "none", "none"), WifiP2pDevice().apply { deviceAddress = "00:00:00:00:00:05"; deviceName = "Support Device" })
    )

    DiscoverViewContent(true, sampleUsers)
}

@Preview(showBackground = true)
@Composable
fun DiscoverViewEmptyStatePreview() {
    DiscoverViewContent(false, emptyList())
}