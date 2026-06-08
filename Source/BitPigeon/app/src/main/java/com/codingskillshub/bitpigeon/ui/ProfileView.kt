package com.codingskillshub.bitpigeon.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.ui.composables.ImageViewOverlay
import com.codingskillshub.bitpigeon.ui.composables.ProfilePictureEditOverlay
import com.codingskillshub.bitpigeon.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileView(
    onEditClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val emailAddress by viewModel.emailAddress.collectAsState()
    val statusLabel by viewModel.statusLabel.collectAsState()
    val profilePictureUri by viewModel.profilePictureUri.collectAsState()
    val isProfilePictureLoading by viewModel.isSavingProfilePicture.collectAsState()
    var selectedProfilePicture by remember { mutableStateOf<AttachmentPreviewData?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let { 
            selectedProfilePicture = viewModel.toAttachmentData(it)
        }
    }

    // Show the overlay if an attachment is selected
    selectedProfilePicture?.let { data ->
            ProfilePictureEditOverlay(
                fileName = data.fileName,
                fileType = data.fileType,
                sourceFileUri = data.fileUri.toString(),
                onDismiss = { selectedProfilePicture = null },
                onSaveCrop = { sourceUri, fileName, cropBounds, displayW, displayH ->
                    viewModel.saveCroppedProfilePicture(
                        sourceUri = sourceUri,
                        cropBounds = cropBounds,
                        displayWidth = displayW,
                        displayHeight = displayH,
                        onSuccess = { selectedProfilePicture = null }
                    )
                }
            )
    }

    ProfileViewContent(
        userName,
        phoneNumber,
        emailAddress,
        statusLabel,
        profilePictureUri.toString(),
        isProfilePictureLoading,
        onEditClick,
        {
            filePickerLauncher.launch("image/*")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewContent(
    userName: String,
    phoneNumber: String,
    emailAddress: String,
    statusLabel: String,
    profilePictureUri: String,
    isProfilePictureLoading: Boolean,
    onEditClick: () -> Unit,
    onEditProfilePictureClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showProfilePictureFullscreen by remember { mutableStateOf<Boolean>(false) }

    // Show the overlay if an attachment is selected
    if (showProfilePictureFullscreen) {
        ImageViewOverlay(
            fileName = "Profile picture",
            fileType = "image/jpg",
            fileUri = profilePictureUri,
            onDismiss = { showProfilePictureFullscreen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture Section
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable( onClick = {
                        showProfilePictureFullscreen = true
                    }),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = profilePictureUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                )
                if (isProfilePictureLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Edit FAB for Picture
            SmallFloatingActionButton(
                onClick = { onEditProfilePictureClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Edit Picture",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 8.dp)) {
                // --- HEADER SECTION ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                
                ProfileItem(label = "Name", value = userName, icon = Icons.Default.Person)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp
                )

                ProfileItem(label = "Status", value = statusLabel, icon = Icons.Default.Info)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp
                )

                ProfileItem(
                    label = "Phone",
                    value = phoneNumber,
                    icon = Icons.Default.Phone
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp
                )

                ProfileItem(label = "Email", value = emailAddress, icon = Icons.Default.Email)
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun ProfileViewPreview() {
    ProfileViewContent("John Doe", "123-456-789", "john.doe@example.com","Hey there", "", false, onEditClick = {}, onEditProfilePictureClick = {})
}
