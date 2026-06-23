package com.codingskillshub.bitpigeon.ui.onboardingscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun WelcomeView(
    title: String,
    description: String = "",
    nextButtonText: String = "Next",
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Image(
                painter = painterResource(id = R.drawable.pigeon_welcome),
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
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(nextButtonText)
            }
        }
    }
}

@Preview(showBackground = true, name = "Welcome Light")
@Composable
fun WelcomeViewPreview() {
    AppTheme("LIGHT") {
        WelcomeView(
            title = "Welcome to BitPigeon",
            description = "Secure, decentralized messaging powered by your local network. Connect with peers without the internet.",
            nextButtonText = "Get Started",
            onNext = {}
        )
    }
}

@Preview(showBackground = true, name = "Welcome Dark")
@Composable
fun WelcomeViewPreviewDark() {
    AppTheme("DARK") {
        WelcomeView(
            title = "Welcome to BitPigeon",
            description = "Secure, decentralized messaging powered by your local network. Connect with peers without the internet.",
            nextButtonText = "Get Started",
            onNext = {}
        )
    }
}
