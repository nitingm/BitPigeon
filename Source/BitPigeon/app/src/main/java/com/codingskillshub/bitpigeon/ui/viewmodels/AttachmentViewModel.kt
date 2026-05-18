package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.models.AttachmentModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttachmentViewModel @Inject constructor(
    val attachmentModel: AttachmentModel
) : ViewModel() {
    private var activeChatGroupId: String = ""
    val photosInChatGroup: StateFlow<List<AttachmentPreviewData>> = attachmentModel.getPhotoAttachmentPreviewDataForChatGroup(activeChatGroupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosInChatGroup: StateFlow<List<AttachmentPreviewData>>  = attachmentModel.getVideoAttachmentPreviewDataForInChatGroup(activeChatGroupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filesInChatGroup: StateFlow<List<AttachmentPreviewData>>  = attachmentModel.getFileAttachmentPreviewDataForInChatGroup(activeChatGroupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun setActiveChatGroupId(chatGroupId: String) {
        activeChatGroupId = chatGroupId
    }
}