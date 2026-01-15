package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val wifiService: WifiCommunicationService
) : ViewModel() {
    val peersList = wifiService.peersList

}