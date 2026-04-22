package com.codingskillshub.bitpigeon.di

import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.common.HashService
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatGroupDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.models.AppSystemModel
import com.codingskillshub.bitpigeon.domain.models.ChatModel
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideChatModel(
        chatDao: ChatDao,
        onlineChatService: OnlineChatService,
        hashService: HashService,
        appSystemModel: AppSystemModel,
        userDao: UserDao,
        configurationService: ConfigurationService
    ): ChatModel = ChatModel(chatDao, onlineChatService, hashService, appSystemModel, userDao, configurationService)

    @Provides
    @Singleton
    fun provideAppSystemModel(
        wifiService: WifiCommunicationService,
        configurationService: ConfigurationService
    ): AppSystemModel = AppSystemModel(wifiService, configurationService)

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