package com.codingskillshub.bitpigeon.ui.composables

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData

@Composable
fun AttachmentPreviewBanner(
    attachedItems: List<AttachmentPreviewData>
) {
    if (attachedItems.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(attachedItems) { item ->
            // Use a fixed width or wrap content to ensure items are side-by-side
            Box(modifier = Modifier.width(200.dp)) {
                AttachmentPreviewEntry(
                    fileName = item.fileName,
                    fileType = item.fileType,
                    fileUri = item.fileUri.toString()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttachmentPreviewBannerPreview() {
    val dummyAttachmentPreviewData = listOf(
        AttachmentPreviewData(
            id = "",
            fileName = "photo.jpg",
            fileType = "image/jpeg",
            fileUri = Uri.parse("content://com.example/photo.jpg")
        ),
        AttachmentPreviewData(
            id = "",
            fileName = "document.pdf",
            fileType = "application/pdf",
            fileUri = Uri.parse("content://com.example/document.pdf")
        )
    )
    MaterialTheme() {
        AttachmentPreviewBanner(attachedItems = dummyAttachmentPreviewData)
    }
}
