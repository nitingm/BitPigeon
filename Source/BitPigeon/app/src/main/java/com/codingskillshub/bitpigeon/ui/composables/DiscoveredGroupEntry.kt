package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User

@Composable
fun DiscoveredGroupEntry(
    clients: List<Client>,
    onClick: (User) -> Unit = {}
) {
    // Extract group owner name from clients list
    val groupOwnerName = clients.find { it.isGroupOwner }?.user?.name ?: "Unknown"

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header with group name (outside border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text(
                text = "${groupOwnerName}'s group",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bordered box containing only the client list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // List of clients
                clients.forEachIndexed { index, client ->
                    ClientEntry(
                        client = client,
                        isGroupOwner = client.isGroupOwner,
                        onClick = onClick,
                        isLast = index == clients.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientEntry(
    client: Client,
    isGroupOwner: Boolean,
    onClick: (User) -> Unit = {},
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier.clickable {
            onClick(client.user)
        },
    ) {
        ListItem(
            // Profile picture (round icon) on the left
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            // Client name
            headlineContent = {
                Text(
                    text = client.user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            // Client device name as supporting text
            supportingContent = {
                Text(
                    text = client.deviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            // GO indicator for group owner
            trailingContent = {
                if (isGroupOwner) {
                    Text(
                        text = "GO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Subtle divider between list items (but not after the last item)
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiscoveredGroupEntryPreview() {
    MaterialTheme {
        val previewClients = listOf(
            Client(
                deviceName = "Nitin's Phone",
                ipAddress = "192.168.1.100",
                isGroupOwner = true,
                user = User(
                    id = "1",
                    name = "Nitin",
                    deviceAddress = "AA:BB:CC:DD:EE:FF",
                    phoneNumber = "9876543210",
                    email = "nitin@example.com"
                )
            ),
            Client(
                deviceName = "Aman's Laptop",
                ipAddress = "192.168.1.101",
                isGroupOwner = false,
                user = User(
                    id = "2",
                    name = "Aman Gupta",
                    deviceAddress = "11:22:33:44:55:66",
                    phoneNumber = "9123456789",
                    email = "aman@example.com"
                )
            ),
            Client(
                deviceName = "Priya's Tablet",
                ipAddress = "192.168.1.102",
                isGroupOwner = false,
                user = User(
                    id = "3",
                    name = "Priya Singh",
                    deviceAddress = "99:88:77:66:55:44",
                    phoneNumber = "9876543211",
                    email = "priya@example.com"
                )
            )
        )

        DiscoveredGroupEntry(
            clients = previewClients
        )
    }
}