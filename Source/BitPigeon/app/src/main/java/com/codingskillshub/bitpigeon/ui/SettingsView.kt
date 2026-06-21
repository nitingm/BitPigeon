package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.ui.composables.SettingItem
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel

@Composable
fun SettingsView(
    navController: NavController,
    appSystemViewModel: AppSystemViewModel
) {

    SettingsViewContent(
        onBackClick = {
            navController.popBackStack()
        },
        onNavigateTo = { route ->
            navController.navigate(route)
        }
    )
}

@Composable
fun SettingsViewContent(
    onBackClick: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            ViewHeader(
                title = "Settings",
                showLeadingImage = false,
                onNavigationClick = {
                    onBackClick()
                },
                showOptionsIcon = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {

            SettingItem(
                primaryText = "Profile",
                secondaryText = "Edit your profile information",
                onClick = {
                    onNavigateTo("profile_edit")
                }
            )
            SettingItem(
                primaryText = "Appearance",
                secondaryText = "Change app theme and appearance",
                onClick = {
                    onNavigateTo("appearance")
                }
            )
            SettingItem(
                primaryText = "Languages",
                secondaryText = "Select your preferred language",
                onClick = {
                    onNavigateTo("languages")
                }
            )
            SettingItem(
                primaryText = "About BitPigeon",
                secondaryText = "Learn more about BitPigeon",
                onClick = {
                    onNavigateTo("about")
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsViewPreview() {
    SettingsViewContent()
}