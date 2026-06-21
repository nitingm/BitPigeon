PROFILE PICTURE CROPPING FEATURE - IMPLEMENTATION GUIDE
========================================================

## What Has Been Implemented

### 1. Dependencies Added (build.gradle.kts)
   - androidx.graphics:graphics-core:1.0.0-alpha03 - for advanced graphics operations
   - androidx.compose.foundation:foundation:1.8.0 - for foundation composables
   - androidx.exifinterface:exifinterface:1.3.7 - for EXIF data handling

### 2. ImageCroppingService (infrastructure/)
   Location: /app/src/main/java/com/codingskillshub/bitpigeon/infrastructure/ImageCroppingService.kt
   
   Features:
   - loadBitmap(uri: Uri): Bitmap? - Loads image from URI with automatic EXIF rotation handling
   - cropAndSaveSquareImage() - Crops to square and saves as JPEG with quality control
   - scaleBitmapToFit() - Scales bitmap while maintaining aspect ratio
   - getRotationDegrees() & rotateBitmap() - Internal EXIF handling
   
   Data Class:
   - CropBounds(left, top, width, height) - Defines crop area in image pixel coordinates

### 3. CropFrameState Utilities (ui/composables/util/)
   Location: /app/src/main/java/com/codingskillshub/bitpigeon/ui/composables/util/CropFrameState.kt
   
   - CropFrameState - Represents crop frame state on display
   - CropBoundsPixels - Crop bounds in actual image pixel coordinates
   - Coordinate translation between display and actual image dimensions

### 4. Enhanced ProfilePictureEditOverlay (ui/composables/)
   Location: /app/src/main/java/com/codingskillshub/bitpigeon/ui/composables/ProfilePictureEditOverlay.kt
   
   New Features:
   - Draggable crop frame (drag gesture to pan)
   - Resizable square crop frame (pinch gesture to resize)
   - Visual guides: green border, corner markers
   - Semi-transparent overlay showing crop area clearly
   - Bottom save button with progress indicator
   - Instruction text: "Drag to pan • Pinch to resize"
   - onSaveCrop callback to handle crop saving

### 5. Extended ProfileViewModel (ui/viewmodels/)
   Location: /app/src/main/java/com/codingskillshub/bitpigeon/ui/viewmodels/ProfileViewModel.kt
   
   New Functionality:
   - saveCroppedProfilePicture() method that handles:
     * Loading image from URI with EXIF handling
     * Cropping to square
     * Saving via FileStorageService
     * Error handling
     * Loading state management
   
   New State Flows:
   - isSavingProfilePicture: StateFlow<Boolean> - UI loading indicator
   - profilePictureError: StateFlow<String?> - Error messages

---

## How to Integrate with Your UI

### Basic Usage Example

```kotlin
// In your ProfileEditView or wherever you open the overlay:
var showProfilePictureEditor by remember { mutableStateOf(false) }

if (showProfilePictureEditor) {
    ProfilePictureEditOverlay(
        saveAsFileName = "profile_pic_${System.currentTimeMillis()}.jpg",
        fileType = "image/jpeg",
        sourceFileUri = selectedImageUri.toString(),
        onDismiss = {
            showProfilePictureEditor = false
        },
        onSaveCrop = { sourceUri, fileName, cropBounds ->
            profileViewModel.saveCroppedProfilePicture(
                sourceUri = sourceUri,
                fileName = fileName,
                cropBounds = cropBounds,
                isPrivateStorage = true,  // Save to app's private storage
                onSuccess = {
                    // Handle success - close overlay, show toast, etc.
                    showProfilePictureEditor = false
                    // Optionally: Reload profile picture from saved location
                }
            )
        }
    )
}
```

### Observing Save State in UI

```kotlin
val isSavingProfilePicture by profileViewModel.isSavingProfilePicture.collectAsState()
val profilePictureError by profileViewModel.profilePictureError.collectAsState()

if (isSavingProfilePicture) {
    // Show loading indicator
    CircularProgressIndicator()
}

profilePictureError?.let { error ->
    // Show error message
    Snackbar(text = "Error saving profile picture: $error")
}
```

---

## Architecture & Data Flow

### Crop Saving Flow:
1. User adjusts crop frame (drag to pan, pinch to resize)
2. Clicks "Save" button
3. ProfilePictureEditOverlay calls onSaveCrop callback
4. ProfileViewModel.saveCroppedProfilePicture() is invoked
5. ImageCroppingService loads bitmap from source URI
6. ImageCroppingService crops to square using CropBounds
7. FileStorageService provides output stream (private or public storage)
8. Cropped image is saved as JPEG
9. FileStorageService finalizes the file (handles MediaStore for public storage)
10. Success callback is invoked

### Coordinates:
- Display coordinates: Where user sees the crop frame on screen (Offset, IntSize)
- Pixel coordinates: Actual image pixel positions (CropBounds, CropBoundsPixels)
- Conversion: Automatically handled via scale factors based on image fitting

---

## Key Design Decisions

1. **Square-Only Cropping**: The crop frame is locked to 1:1 aspect ratio, ensuring perfect square output
2. **Private Storage Default**: Profile pictures are saved to app's private storage for user privacy
3. **EXIF Handling**: Automatic rotation based on EXIF orientation data
4. **Bitmap Recycling**: Automatic cleanup of intermediate bitmaps to prevent memory leaks
5. **Quality Control**: JPEG quality set to 90 for good balance between quality and file size
6. **Gesture Handling**: 
   - Drag: Pans the crop frame
   - Pinch: Resizes the crop frame (min 50px, max image dimension)

---

## File Locations & Structure

```
app/src/main/java/com/codingskillshub/bitpigeon/
├── infrastructure/
│   ├── ImageCroppingService.kt        (NEW)
│   ├── FileStorageService.kt          (UNCHANGED)
│   └── ... other services
├── ui/
│   ├── composables/
│   │   ├── ProfilePictureEditOverlay.kt   (ENHANCED)
│   │   ├── util/
│   │   │   └── CropFrameState.kt          (NEW)
│   │   └── ... other composables
│   ├── viewmodels/
│   │   └── ProfileViewModel.kt            (EXTENDED)
│   └── ... other UI files
└── ... other packages
```

---

## Configuration Options

### In ProfileViewModel.saveCroppedProfilePicture():
- `isPrivateStorage`: Boolean (default: true) - Use private app storage or public Pictures
- `quality`: Int (default: 90) - JPEG compression quality (0-100)

### In ImageCroppingService.cropAndSaveSquareImage():
- `quality`: Int parameter for fine-tuning JPEG compression

---

## Error Handling

The implementation includes comprehensive error handling:

1. **Loading Errors**: If image fails to load from URI
2. **Cropping Errors**: If bitmap operations fail
3. **File I/O Errors**: Handled by FileStorageService
4. **EXIF Errors**: Gracefully degraded if EXIF data unavailable
5. **Memory Errors**: Bitmap recycling prevents memory issues

All errors are captured in `profilePictureError` StateFlow for UI display.

---

## Testing Notes

To test the implementation:

1. Open ProfileEditView
2. Trigger profile picture selection (implementation-specific)
3. ProfilePictureEditOverlay appears with image
4. Test dragging: Click and drag to pan crop frame
5. Test pinching: Use pinch gesture to resize crop frame (size indicator at bottom helps)
6. Verify crop frame stays within image bounds
7. Click "Save" and verify:
   - Loading indicator appears
   - File is saved successfully
   - UI updates accordingly
   - Error messages appear if something fails

---

## Next Steps (Optional Enhancements)

1. **Crop History**: Store cropped images with timestamps
2. **Aspect Ratio Toggle**: Allow square or custom ratios
3. **Brightness/Contrast Adjustment**: Add filters in crop editor
4. **Undo/Redo**: Multi-step undo for crop adjustments
5. **Gallery Integration**: Browse from gallery directly
6. **Compression Settings**: UI controls for quality selection
7. **Preview Panel**: Show preview of final square image before saving

---

## Troubleshooting

### Image Not Loading:
- Verify URI is valid and accessible
- Check permissions (READ_EXTERNAL_STORAGE, READ_MEDIA_IMAGES)
- Verify Coil is properly configured

### Crop Frame Not Moving:
- Ensure gesture detection is enabled (check modifier chain)
- Verify imageSize is properly initialized in onSizeChanged

### File Not Saving:
- Check storage permissions (WRITE_EXTERNAL_STORAGE for public, handled automatically for private)
- Verify FileStorageService is injected correctly
- Check error in profilePictureError StateFlow

### Out of Memory:
- ImageCroppingService automatically recycles bitmaps
- Consider scaling large images down first
- Increase heap size if necessary in build config

---

## Dependencies Summary

```toml
# Added:
androidx-graphics = "1.0.0-alpha03"
androidx-compose-foundation = "1.8.0"
androidx-exifinterface = "1.3.7"

# Already present:
coil = "3.0.4"
androidx-core-ktx = "1.17.0"
androidx-datastore = "1.1.1"
```

---

Implementation completed and ready for integration!

