package com.codingskillshub.bitpigeon.ui

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredGroupEntry
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredPeerEntry
import com.codingskillshub.bitpigeon.ui.viewmodels.DiscoverViewModel

@Composable
fun DiscoverView(
    navController: NavController,
    discoverViewModel: DiscoverViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val discoveredUsers by discoverViewModel.discoveredUsers.collectAsState()
    val usersList = discoveredUsers.values.toList()
    val availablePeerClients by discoverViewModel.availableClients.collectAsState()

    discoverViewModel.onChatGroupInvoked = { groupId ->
        navController.navigate("chatview/$groupId")
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = modifier.weight(1f), // Takes up remaining space,
            // Adds spacing at the top and bottom of the list
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (availablePeerClients.isNotEmpty()) {
                item {
                    DiscoveredGroupEntry(
                        availablePeerClients,
                        onClick = { user ->
                            discoverViewModel.createAndOpenDirectChat(user)
                        }
                    )
                }
            }


            // 'items' handles the recycling and lazy loading automatically
            items(
                items = usersList,
                // Providing a 'key' helps Compose optimize list updates/reordering
                key = { (user, device) -> device.deviceAddress }
            ) { (user, device) ->
                DiscoveredPeerEntry(
                    name = user.name,
                    statusString = "${user.email} (${device.deviceName})",
                    onClick = { discoverViewModel.connectToPeer(device) }
                )
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DiscoverViewPreview() {
//    val samplePeers = listOf(
//        ChatData("1", "Aman Gupta", "Got the files!", "27/12/2025"),
//        ChatData("2", "John Doe", "Are you online?", "26/12/2025"),
//        ChatData("3", "Project Group", "Meeting at 5 PM", "25/12/2025"),
//        ChatData("4", "Mama", "Call me later", "24/12/2025"),
//        ChatData("5", "BitPigeon Support", "Welcome to the app!", "20/12/2025")
//    )
//
//    MaterialTheme {
//        DiscoverView(
//            chatList = samplePeers,
//            onChatClick = { /* Handle navigation */ }
//        )
//    }
//}