package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@Composable
fun ChatEntry(
    name: String,
    lastMessage: String,
    timestamp: String, // Expected in dd/mm/yyyy format
    profilePictureUri: String,
    isOnline: Boolean = false,
    onClick: () -> Unit = {},
    onProfilePictureClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        ListItem(
            // 1. Profile picture (round icon) on the left
            leadingContent = {
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onProfilePictureClick),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Picture",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }
                    // Green online badge at bottom-right
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color = Color(red = 0x00, green = 0xFF, blue = 0x00, alpha = 0xFF))
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            },
            // 2. Primary text for Person name
            headlineContent = {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            // 3. Secondary text for last message (small and faint)
            supportingContent = {
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            // 4. Timestamp on the right
            trailingContent = {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        )
        // Subtle divider between list items
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatEntryOnlinePreview() {
    MaterialTheme {
        ChatEntry(
            name = "Aman Gupta",
            lastMessage = "Hey, did you send the project files?",
            timestamp = "27/12/2025",
            profilePictureUri = "",
            isOnline = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatEntryOfflinePreview() {
    MaterialTheme {
        ChatEntry(
            name = "Aman Gupta",
            lastMessage = "Hey, did you send the project files?",
            timestamp = "27/12/2025",
            profilePictureUri = "",
            isOnline = false
        )
    }
}

