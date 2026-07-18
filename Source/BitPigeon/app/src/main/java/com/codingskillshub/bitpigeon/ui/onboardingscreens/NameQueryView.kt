package com.codingskillshub.bitpigeon.ui.onboardingscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingskillshub.bitpigeon.R
import com.codingskillshub.bitpigeon.ui.theme.AppTheme

@Composable
fun NameQueryView(
    title: String,
    description: String = "",
    nextButtonText: String = "Next",
    showSkip: Boolean = false,
    isSkipEnabled: Boolean = false,
    onSkip: () -> Unit = {},
    onNext: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var tempName by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Image(
                    painter = painterResource(id = R.drawable.pigeon_phone),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showSkip) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        enabled = isSkipEnabled
                    ) {
                        Text("Skip")
                    }
                }

                Button(
                    onClick = { onNext(tempName) },
                    modifier = Modifier.weight(if (showSkip) 1f else 2f),
                    enabled = tempName.isNotBlank()
                ) {
                    Text(nextButtonText)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Name Query Light")
@Composable
fun NameQueryViewPreview() {
    AppTheme("LIGHT") {
        NameQueryView(
            title = "What's your name?",
            description = "This name will be visible to your peers when you connect.",
            showSkip = true,
            isSkipEnabled = false,
            onNext = {}
        )
    }
}

@Preview(showBackground = true, name = "Name Query Dark")
@Composable
fun NameQueryViewPreviewDark() {
    AppTheme("DARK") {
        NameQueryView(
            title = "What's your name?",
            description = "This name will be visible to your peers when you connect.",
            showSkip = true,
            isSkipEnabled = false,
            onNext = {}
        )
    }
}
