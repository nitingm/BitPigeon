package com.codingskillshub.bitpigeon.ui

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codingskillshub.bitpigeon.ui.composables.DiscoveredPeerEntry
import com.codingskillshub.bitpigeon.ui.viewmodels.DiscoverViewModel

@Composable
fun DiscoverView(
    onChatClick: (WifiP2pDevice) -> Unit,
    discoverViewModel: DiscoverViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val peersList by discoverViewModel.peersList.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = modifier.weight(1f), // Takes up remaining space,
            // Adds spacing at the top and bottom of the list
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 'items' handles the recycling and lazy loading automatically
            items(
                items = peersList,
                // Providing a 'key' helps Compose optimize list updates/reordering
                key = { peer: WifiP2pDevice -> peer.deviceAddress }
            ) { peer: WifiP2pDevice ->
                DiscoveredPeerEntry(
                    name = peer.deviceName,
                    statusString = peer.deviceAddress,
                    onClick = { onChatClick(peer) }
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