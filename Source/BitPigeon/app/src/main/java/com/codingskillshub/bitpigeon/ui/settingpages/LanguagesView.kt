package com.codingskillshub.bitpigeon.ui.settingpages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.ui.composables.SettingItem
import com.codingskillshub.bitpigeon.ui.composables.SettingRadioButton
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader

@Composable
fun LanguagesView(
    navController: NavController
) {

    LanguagesViewContent(
        languages = listOf("English"),
        currentLanguage = "English",
        onLanguageClick = { language ->
            // Handle language selection
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
}

@Composable
fun LanguagesViewContent(
    languages: List<String>,
    currentLanguage: String,
    onLanguageClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            ViewHeader(
                title = "Languages",
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
            languages.forEach { language ->
                SettingItem(
                    primaryText = language,
                    onClick = { onLanguageClick(language) },
                    trailingContent = {
                        SettingRadioButton(
                            selected = language == currentLanguage,
                            onClick = { onLanguageClick(language) }
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguagesViewPreview() {
    LanguagesViewContent(
        languages = listOf("English", "Spanish", "French"),
        currentLanguage = "English"
    )
}