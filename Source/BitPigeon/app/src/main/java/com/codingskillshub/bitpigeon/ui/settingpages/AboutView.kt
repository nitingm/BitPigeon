package com.codingskillshub.bitpigeon.ui.settingpages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.R
import com.codingskillshub.bitpigeon.ui.composables.SettingItem
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel

@Composable
fun AboutView(
    navController: NavController,
    appSystemViewModel: AppSystemViewModel
) {
    val version = remember { appSystemViewModel.getAppVersionDetail() }

    AboutViewContent(
        appName = "BitPigeon",
        appVersion = version,
        onBackClick = { navController.popBackStack() },
        onPrivacyClick = {
            appSystemViewModel.openPrivacyPolicy()
        },
        onTermsClick = {
            appSystemViewModel.openTermsAndConditions()
        },
        onLicenseClick = {
            appSystemViewModel.openLicenseAndCertificates()
        }
    )
}

@Composable
fun AboutViewContent(
    appName: String,
    appVersion: String,
    onBackClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.bitpigeon_ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(id = R.drawable.bitpigeon_ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Name
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // App Version
        Text(
            text = appVersion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Options List
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingItem(
                primaryText = "Terms and conditions",
                onClick = onTermsClick
            )
            SettingItem(
                primaryText = "Privacy policy",
                onClick = onPrivacyClick
            )
            SettingItem(
                primaryText = "Copyright License",
                onClick = onLicenseClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutViewPreview() {
    MaterialTheme {
        AboutViewContent(
            "BitPigeon", "0.0.0"
        )
    }
}
