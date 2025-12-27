package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageBubble(
    senderName: String,
    messageText: String,
    timestamp: String, // Expected in hh:mm format
    isSentByMe: Boolean,
    showHeader: Boolean = true,
    imageThumbnails: List<Int> = emptyList(), // Resource IDs for images
    modifier: Modifier = Modifier
) {
    // 1. Alignment Logic: Sent = End (Right), Received = Start (Left)
    val horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    val bubbleColor =
        if (isSentByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isSentByMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        // 2. Header (Sender Name and Role/Status)
        if (showHeader) {
            Row(
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSentByMe) "You" else senderName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSentByMe) "Sent" else "Received",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            }
        }

        // 3. The Message Bubble
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isSentByMe) 16.dp else 0.dp, // Tail for received
                bottomEnd = if (isSentByMe) 0.dp else 16.dp   // Tail for sent
            ),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp) // Prevents bubble from stretching full width
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                // Image Thumbnails (if any)
                if (imageThumbnails.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        items(imageThumbnails) { _ ->
                            // Placeholder for thumbnail (icon or empty box)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🖼️", fontSize = 20.sp) // Emoji placeholder
                            }
                        }
                    }
                }

                // Message Body
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )

                // 4. Timestamp at bottom right
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    MaterialTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            MessageBubble(
                senderName = "Aman Gupta",
                messageText = "Check out these screenshots 🚀",
                timestamp = "14:20",
                isSentByMe = false,
                imageThumbnails = listOf(1, 2)
            )

            Spacer(modifier = Modifier.height(8.dp))

            MessageBubble(
                senderName = "Me",
                messageText = "Looks great! I'll get started on the Wi-Fi module now. 👍",
                timestamp = "14:22",
                isSentByMe = true
            )
        }
    }
}

