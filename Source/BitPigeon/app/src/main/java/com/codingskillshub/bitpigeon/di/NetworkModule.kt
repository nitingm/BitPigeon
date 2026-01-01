package com.codingskillshub.bitpigeon.di

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.codingskillshub.bitpigeon.services.WifiCommunicationService

@Module
@InstallIn(SingletonComponent::class) // This makes the dependencies live as long as the App
object NetworkModule {

    @Provides
    @Singleton // This ensures only ONE instance is created
    fun provideWifiP2pManager(@ApplicationContext context: Context): WifiP2pManager? {
        return context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    @Provides
    @Singleton
    fun provideWifiP2pChannel(
        @ApplicationContext context: Context,
        manager: WifiP2pManager?
    ): WifiP2pManager.Channel? {
        return manager?.initialize(context, context.mainLooper, null)
    }

    @Provides
    @Singleton
    fun provideWifiCommunicationService(
        manager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        context: Context
    ): WifiCommunicationService? {
        return WifiCommunicationService(manager, channel, context)
    }
}