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
        }
    )
}

@Composable
fun SettingsViewContent(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            ViewHeader(
                title = "Settings",
                showLeadingImage = false,
                onNavigationClick = {
                    onBackClick()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {

        }
    }
}

@Composable
fun SettingItem(

) {

}

@Preview(showBackground = true)
@Composable
fun SettingsViewPreview() {
    SettingsViewContent()
}