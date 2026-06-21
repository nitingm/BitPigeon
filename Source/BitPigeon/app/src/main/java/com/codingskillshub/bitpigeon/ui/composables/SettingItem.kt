package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

/**
 * A reusable settings row component that follows Material 3 design.
 *
 * @param primaryText The main title of the setting.
 * @param secondaryText Optional subtitle or description below the primary text.
 * @param onClick Callback when the row is clicked.
 * @param leadingIcon Optional composable icon at the start of the row.
 * @param trailingContent Optional composable control (e.g., switch, button) at the end of the row.
 */
@Composable
fun SettingItem(
    primaryText: String,
    secondaryText: String? = null,
    onClick: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = secondaryText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = leadingIcon,
        trailingContent = trailingContent,
        modifier = Modifier.clickable { onClick() }
    )
}

/**
 * A standard icon for use in [SettingItem].
 *
 * @param icon The Material icon (ImageVector) to display.
 * @param contentDescription Optional description for accessibility.
 */
@Composable
fun SettingIcon(
    icon: ImageVector,
    contentDescription: String? = null
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * A standard switch control for use in [SettingItem].
 * It is scaled down slightly (0.8f) to appear slimmer.
 *
 * @param checked Whether the switch is on or off.
 * @param onCheckedChange Callback when the switch state changes.
 */
@Composable
fun SettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.scale(0.8f)
    )
}

@Composable
fun SettingRadioButton(
    selected: Boolean,
    onClick: () -> Unit = {}
) {
    RadioButton(
        selected = selected,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
fun SettingItemPreview() {
    MaterialTheme {
        Column {
            SettingItem(
                primaryText = "Wi-Fi Direct",
                secondaryText = "Enable Wi-Fi for messaging",
                leadingIcon = { SettingIcon(icon = Icons.Default.Wifi) },
                trailingContent = { SettingSwitch(checked = true) }
            )
            SettingItem(
                primaryText = "Notifications",
                leadingIcon = { SettingIcon(icon = Icons.Default.Notifications) },
                trailingContent = { SettingSwitch(checked = false) }
            )
            SettingItem(
                primaryText = "Account Settings",
                secondaryText = "Manage your profile and privacy"
            )
            SettingItem(
                primaryText = "About",
                leadingIcon = { SettingIcon(icon = Icons.Default.Notifications) },
            )
            SettingItem(
                primaryText = "Selected Item",
                trailingContent = { SettingRadioButton(selected = true) }
            )
        }
    }
}
