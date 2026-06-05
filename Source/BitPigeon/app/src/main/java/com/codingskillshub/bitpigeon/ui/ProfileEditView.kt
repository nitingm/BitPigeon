package com.codingskillshub.bitpigeon.ui

import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileEditView(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by profileViewModel.userName.collectAsState()
    val phoneNumber by profileViewModel.phoneNumber.collectAsState()
    val emailAddress by profileViewModel.emailAddress.collectAsState()
    val statusLabel by profileViewModel.statusLabel.collectAsState()
    ProfileEditViewContent(
        name = userName ,
        phoneNumber = phoneNumber,
        emailAddress = emailAddress,
        statusLabel = statusLabel,
        onNavigateBack = onNavigateBack,
        onSaveClick = { updatedUser ->
            profileViewModel.updateUserName(updatedUser.name)
            profileViewModel.updatePhoneNumber(updatedUser.phoneNumber)
            profileViewModel.updateEmailAddress(updatedUser.email)
            profileViewModel.updateStatus(updatedUser.status)

            onNavigateBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditViewContent(
    name: String,
    phoneNumber: String,
    emailAddress: String,
    statusLabel: String,
    onNavigateBack: () -> Unit,
    onSaveClick: (User) -> Unit
) {
    // Local state for form fields
    var localName by remember { mutableStateOf(name) }
    var localPhoneNumber by remember { mutableStateOf(phoneNumber) }
    var localEmail by remember { mutableStateOf(emailAddress) }
    var localStatus by remember { mutableStateOf(statusLabel) }

    // Update state when parameters change
    LaunchedEffect(name, phoneNumber, emailAddress, statusLabel) {
        localName = name
        localPhoneNumber = phoneNumber
        localEmail = emailAddress
        localStatus = statusLabel
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // Create updated user object and pass back
                        onSaveClick(
                            User(
                                id = "none",
                                name = localName,
                                deviceAddress = "none",
                                phoneNumber = localPhoneNumber,
                                email = localEmail,
                                status = localStatus
                            )
                        )
                    }) {
                        Text("Save", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Form Fields
            EditField(
                label = "Full Name",
                value = localName,
                onValueChange = { localName = it },
                icon = Icons.Default.Badge
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditField(
                label = "Status",
                value = localStatus,
                onValueChange = { localStatus = it },
                icon = Icons.Default.Info,
                placeholder = "Hey there! I am using BitPigeon"
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditField(
                label = "Phone Number",
                value = localPhoneNumber,
                onValueChange = { localPhoneNumber = it },
                icon = Icons.Default.Phone
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditField(
                label = "Email Address",
                value = localEmail,
                onValueChange = { localEmail = it },
                icon = Icons.Default.Email
            )

            Text(
                text = "This information will be visible to peers you connect with via Wi-Fi Direct.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    top = 24.dp,
                    start = 8.dp,
                    end = 8.dp
                )
            )
        }
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = true
    )
}

@Preview
@Composable
fun ProfileEditViewPreview() {
    ProfileEditViewContent("John Doe", "729427923437","johndoae@gail.com","Hey there!!!", {},{})
}