package com.codingskillshub.bitpigeon.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.codingskillshub.bitpigeon.domain.interfaces.BitPigeonDatabase
import com.codingskillshub.bitpigeon.domain.interfaces.dao.AttachmentDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatGroupDao

import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BitPigeonDatabase {
        return Room.databaseBuilder(
            context,
            BitPigeonDatabase::class.java,
            "bitpigeon_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideChatDao(db: BitPigeonDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideUserDao(db: BitPigeonDatabase): UserDao = db.userDao()

    @Provides
    fun provideChatGroupDao(db: BitPigeonDatabase): ChatGroupDao = db.chatGroupDao()

    @Provides
    fun provideAttachmentDao(db: BitPigeonDatabase): AttachmentDao = db.attachmentDao()
}