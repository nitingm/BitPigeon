package com.codingskillshub.bitpigeon.domain.models

import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSystemModel @Inject constructor(
    private val wifiService: WifiCommunicationService,
    private val configurationService: ConfigurationService
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var myUserId = "DefaultUser"

    init {
        modelScope.launch {
            initializeUserId()
            runBlocking {
                myUserId = configurationService.userIdFlow.first()?: "DefaultUser"
            }
        }
    }

    private suspend fun initializeUserId() {
        configurationService.generateAndSaveUserId()
    }

    fun getDeviceAddress(): StateFlow<String> {
        return wifiService.deviceAddress
    }

    fun getMyUserId(): String {
        return myUserId
    }
}