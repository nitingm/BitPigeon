package com.codingskillshub.bitpigeon.ui.onboardingscreens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.codingskillshub.bitpigeon.R

import com.codingskillshub.bitpigeon.ui.theme.AppTheme

@Composable
fun ProfilePictureQueryView(
    title: String,
    description: String = "",
    nextButtonText: String = "Next",
    selectedImageUri: String,
    showSkip: Boolean = false,
    isSkipEnabled: Boolean = false,
    onSkip: () -> Unit = {},
    onNext: () -> Unit = {},
    onProfilePhotoSelected: (Uri) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onProfilePhotoSelected(it)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
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
                    painter = painterResource(id = R.drawable.pigeon_wifi_on),
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

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Surface(
                                modifier = Modifier.size(150.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.pigeon_welcome),
                                        contentDescription = "Pick Image",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    )
                }

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
                    onClick = { onNext() },
                    modifier = Modifier.weight(if (showSkip) 1f else 2f),
                    enabled = selectedImageUri.isNotEmpty()
                ) {
                    Text(nextButtonText)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Profile Picture Light")
@Composable
fun ProfilePictureQueryViewPreview() {
    AppTheme("LIGHT") {
        ProfilePictureQueryView(
            title = "Select a profile picture",
            description = "Choose a picture so friends can recognize you.",
            nextButtonText = "Next",
            selectedImageUri = "",
            showSkip = true,
            isSkipEnabled = true,
            onSkip = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true, name = "Profile Picture Dark")
@Composable
fun ProfilePictureQueryViewPreviewDark() {
    AppTheme("DARK") {
        ProfilePictureQueryView(
            title = "Select a profile picture",
            description = "Choose a picture so friends can recognize you.",
            nextButtonText = "Next",
            selectedImageUri = "",
            showSkip = true,
            isSkipEnabled = true,
            onSkip = {},
            onNext = {},
        )
    }
}
