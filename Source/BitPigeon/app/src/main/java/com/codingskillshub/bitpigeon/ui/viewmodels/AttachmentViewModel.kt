package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.models.AttachmentModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttachmentViewModel @Inject constructor(
    private val attachmentModel: AttachmentModel,
    savedStateHandle: SavedStateHandle // Prefer getting ID from navigation args
) : ViewModel() {
    private val chatId: String = savedStateHandle["chatId"] ?: ""

    val photosInChatGroup = attachmentModel.getPhotoAttachmentPreviewDataForChatGroup(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosInChatGroup = attachmentModel.getVideoAttachmentPreviewDataForInChatGroup(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filesInChatGroup = attachmentModel.getFileAttachmentPreviewDataForInChatGroup(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}