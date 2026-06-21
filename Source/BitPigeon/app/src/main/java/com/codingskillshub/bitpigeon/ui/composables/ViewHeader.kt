package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewHeader(
    title: String,
    subtitle: String? = null,
    onTitleRowClicked: () -> Unit = {},
    // Navigation (Left)
    showNavigationIcon: Boolean = true,
    navigationIconEnabled: Boolean = true,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    onNavigationClick: () -> Unit = {},

    // Profile/Leading Image
    showLeadingImage: Boolean = false,
    leadingImageEnabled: Boolean = true,
    leadingImageUri: String? = null, // Resource ID for the image
    onLeadingImageClick: () -> Unit = {},

    // Action Icons (Right)
    optionalIcon1: ImageVector? = null,
    onOptionalIcon1Click: () -> Unit = {},
    optionalIcon2: ImageVector? = null,
    onOptionalIcon2Click: () -> Unit = {},

    // Options Menu
    showOptionsIcon: Boolean = true,
    optionsMenu: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { onTitleRowClicked() }
            ) {
                // Profile Image before text
                if (showLeadingImage) {
                    IconButton(
                        onClick = onLeadingImageClick,
                        enabled = leadingImageEnabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (leadingImageUri != null) {
                            SubcomposeAsyncImage(
                                model = leadingImageUri,
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
                        } else {
                            // Fallback if no image res provided
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Text Content
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(
                    onClick = onNavigationClick,
                    enabled = navigationIconEnabled
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            // Optional Icon 1
            optionalIcon1?.let {
                IconButton(onClick = onOptionalIcon1Click) {
                    Icon(imageVector = it, contentDescription = "Action 1")
                }
            }
            // Optional Icon 2
            optionalIcon2?.let {
                IconButton(onClick = onOptionalIcon2Click) {
                    Icon(imageVector = it, contentDescription = "Action 2")
                }
            }
            // Standard Options Icon
            if (showOptionsIcon) {
                Box {
                    IconButton(onClick = {
                        isMenuExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options"
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        optionsMenu { isMenuExpanded = false }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ViewHeaderPreview() {
    MaterialTheme {
        ViewHeader(
            title = "Aman Gupta",
            subtitle = "Online",
            showLeadingImage = true,
            onNavigationClick = { println("Back clicked") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleTextViewHeaderPreview() {
    MaterialTheme {
        // Example 2: Settings Header (No subtitle, no image, custom navigation icon)
        ViewHeader(
            title = "Settings",
            showLeadingImage = false,
            showOptionsIcon = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppViewHeaderPreview() {
    MaterialTheme {
        // Example 2: Settings Header (No subtitle, no image, custom navigation icon)
        ViewHeader(
            title = "BitPigeon",
            showLeadingImage = false,
            showOptionsIcon = false,
            showNavigationIcon = false
        )
    }
}
