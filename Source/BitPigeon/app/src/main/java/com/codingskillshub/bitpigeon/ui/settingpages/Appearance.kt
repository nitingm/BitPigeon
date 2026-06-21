package com.codingskillshub.bitpigeon.ui.settingpages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.ui.composables.SettingItem
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel

@Composable
fun AppearanceView(
    navController: NavController,
    appSystemViewModel: AppSystemViewModel
) {
    val currentAppTheme by appSystemViewModel.appTheme.collectAsStateWithLifecycle()
    AppearanceViewContent(
        currentAppTheme = currentAppTheme,
        availableAppThemes = appSystemViewModel.availableAppThemes,
        onBackClick = {
            navController.popBackStack()
        },
        onThemeSelected = {
            appSystemViewModel.changeAppTheme(it)
        }
    )
}

@Composable
fun AppearanceViewContent(
    currentAppTheme: Pair<String, String>,
    availableAppThemes: List<Pair<String, String>> = emptyList(),
    onBackClick: () -> Unit = {},
    onThemeSelected: (Pair<String, String>) -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            ViewHeader(
                title = "Appearance",
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
                primaryText = "Theme",
                secondaryText = currentAppTheme.second,
                onClick = {
                    showDialog = true
                }
            )
        }
    }

    if (showDialog) {
        ThemeSelectionDialog(
            initialTheme = currentAppTheme,
            availableAppThemes = availableAppThemes,
            onDismiss = { showDialog = false },
            onThemeSelected = {
                onThemeSelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    initialTheme: Pair<String,String>,
    availableAppThemes: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onThemeSelected: (Pair<String, String>) -> Unit = {}
) {
    var selectedOption by remember { mutableStateOf(initialTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Theme", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(Modifier.selectableGroup()) {
                availableAppThemes.forEach { text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (text == selectedOption),
                                onClick = { selectedOption = text },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = null
                        )
                        Text(
                            text = text.second,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onThemeSelected(selectedOption) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AppearanceViewPreview() {
    val availableAppThemes: List<Pair<String, String>> = listOf(
        "LIGHT" to "Light",
        "DARK" to "Dark",
        "SYSTEM_DEFAULT" to "System Default"
    )
    AppearanceViewContent(
        currentAppTheme = availableAppThemes[0],
        availableAppThemes = availableAppThemes,
        {},
        {}
    )
}
