package com.codingskillshub.bitpigeon.di

import android.content.Context
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.common.HashService
import com.codingskillshub.bitpigeon.domain.interfaces.dao.AttachmentDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatGroupDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.models.AppSystemModel
import com.codingskillshub.bitpigeon.domain.models.AttachmentModel
import com.codingskillshub.bitpigeon.domain.models.ChatModel
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import com.codingskillshub.bitpigeon.domain.services.FileTransferService
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import com.codingskillshub.bitpigeon.infrastructure.ImageCroppingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAttachmentModel(
        fileTransferService: FileTransferService,
        appSystemModel: AppSystemModel,
        chatGroupDao: ChatGroupDao,
        attachmentDao: AttachmentDao,
        onlineChatService: OnlineChatService,
        wifiCommunicationService: WifiCommunicationService,
        fileStorageService: FileStorageService,
        @ApplicationContext context: Context
    ): AttachmentModel = AttachmentModel(fileTransferService, appSystemModel, chatGroupDao, attachmentDao, onlineChatService, wifiCommunicationService, fileStorageService,context)

    @Provides
    @Singleton
    fun provideChatModel(
        chatDao: ChatDao,
        onlineChatService: OnlineChatService,
        hashService: HashService,
        attachmentModel: AttachmentModel,
        appSystemModel: AppSystemModel,
        userDao: UserDao,
        configurationService: ConfigurationService
    ): ChatModel = ChatModel(chatDao, onlineChatService, hashService, attachmentModel,appSystemModel, userDao, configurationService)

    @Provides
    @Singleton
    fun provideAppSystemModel(
        userDao: UserDao,
        imageCroppingService: ImageCroppingService,
        fileStorageService: FileStorageService,
        onlineChatService: OnlineChatService,
        wifiService: WifiCommunicationService,
        configurationService: ConfigurationService
    ): AppSystemModel = AppSystemModel(userDao, imageCroppingService, fileStorageService, onlineChatService, wifiService, configurationService)

    @Provides
    @Singleton
    fun provideConversationModel(
        chatModel: ChatModel,
        chatGroupDao: ChatGroupDao,
        userDao: UserDao,
        onlineChatService: OnlineChatService,
        configurationService: ConfigurationService,
        appSystemModel: AppSystemModel,
        hashService: HashService
    ): ConversationModel = ConversationModel(chatModel, chatGroupDao, userDao, onlineChatService, configurationService, appSystemModel, hashService)
}