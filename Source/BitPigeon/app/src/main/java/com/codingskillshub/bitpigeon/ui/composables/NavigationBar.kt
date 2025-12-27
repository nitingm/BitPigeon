package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

// 1. Data model to make adding future icons easy
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BitPigeonNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // 2. Define the list of icons. You can add more items here later. 
    val items = listOf(
        NavItem("Chats", Icons.Default.Chat, "chats_screen"),
        NavItem("Settings", Icons.Default.Settings, "settings_screen")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp // Standard Material 3 look
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                alwaysShowLabel = true // Set to false if you want label only on selection
            )
        }
    }
}

@Preview
@Composable
fun NavigationBarPreview() {
    MaterialTheme {
        BitPigeonNavigationBar(
            currentRoute = "chats_screen",
            onNavigate = {}
        )
    }
}
