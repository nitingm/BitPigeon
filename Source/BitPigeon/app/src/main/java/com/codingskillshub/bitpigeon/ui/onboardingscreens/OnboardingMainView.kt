package com.codingskillshub.bitpigeon.ui.onboardingscreens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.ui.composables.ProfilePictureEditOverlay
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ProfileViewModel

enum class OnboardingStep {
    WELCOME,
    NAME_QUERY,
    PROFILE_PICTURE_QUERY
}

@Composable
fun OnboardingMainView(
    navController: NavController,
    appSystemViewModel: AppSystemViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    val onComplete = {
        appSystemViewModel.completeOnboarding()
        navController.navigate("main") {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    val profilePictureUri by profileViewModel.profilePictureUri.collectAsState()
    var selectedProfilePicture by remember { mutableStateOf<AttachmentPreviewData?>(null) }

    // Show the overlay if an attachment is selected
    selectedProfilePicture?.let { data ->
        ProfilePictureEditOverlay(
            fileName = data.fileName,
            fileType = data.fileType,
            sourceFileUri = data.fileUri.toString(),
            onDismiss = { selectedProfilePicture = null },
            onSaveCrop = { sourceUri, fileName, cropBounds, displayW, displayH ->
                profileViewModel.saveCroppedProfilePicture(
                    sourceUri = sourceUri,
                    cropBounds = cropBounds,
                    displayWidth = displayW,
                    displayHeight = displayH,
                    onSuccess = { selectedProfilePicture = null }
                )
            }
        )
    }

    when (currentStep) {
        OnboardingStep.WELCOME -> {
            WelcomeView(
                title = "Welcome to BitPigeon",
                description = "Secure, decentralized messaging powered by your local network. Connect with peers without the internet.",
                nextButtonText = "Get Started",
                onNext = { currentStep = OnboardingStep.NAME_QUERY }
            )
        }
        OnboardingStep.NAME_QUERY -> {
            NameQueryView(
                title = "What's your name?",
                description = "This name will be visible to your peers when you connect.",
                showSkip = true,
                isSkipEnabled = false,
                onNext = { name ->
                    profileViewModel.updateUserName(name)
                    currentStep = OnboardingStep.PROFILE_PICTURE_QUERY
                }
            )
        }
        OnboardingStep.PROFILE_PICTURE_QUERY -> {
            ProfilePictureQueryView(
                title = "Select a profile picture",
                description = "Choose a picture so friends can recognize you.",
                showSkip = true,
                isSkipEnabled = true,
                selectedImageUri = profilePictureUri.toString(),
                onSkip = {
                    onComplete()
                },
                onNext = {
                    onComplete()
                },
                onProfilePhotoSelected = { uri ->
                    selectedProfilePicture = profileViewModel.toAttachmentData(uri)
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Welcome Screen")
@Composable
fun OnboardingWelcomePreview() {
    WelcomeView(
        title = "Welcome to BitPigeon",
        description = "Secure, decentralized messaging powered by your local network. Connect with peers without the internet.",
        nextButtonText = "Get Started",
        onNext = {}
    )
}

@Preview(showBackground = true, name = "Name Query Screen")
@Composable
fun OnboardingNameQueryPreview() {
    NameQueryView(
        title = "What's your name?",
        description = "This name will be visible to your peers when you connect.",
        showSkip = true,
        isSkipEnabled = false,
        onNext = {}
    )
}

@Preview(showBackground = true, name = "Profile Picture Screen")
@Composable
fun OnboardingProfilePicturePreview() {
    ProfilePictureQueryView(
        title = "Select a profile picture",
        description = "Choose a picture so friends can recognize you.",
        selectedImageUri = "",
        showSkip = true,
        isSkipEnabled = true,
        onSkip = {},
        onNext = {}
    )
}
