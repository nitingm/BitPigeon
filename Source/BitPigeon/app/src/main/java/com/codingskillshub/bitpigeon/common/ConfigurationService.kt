package com.codingskillshub.bitpigeon.common

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "local_configuration")
    private val USER_NAME = stringPreferencesKey("user_name")
    private val PHONE_NUMBER = stringPreferencesKey("phone_number")
    private val EMAIL_ADDRESS = stringPreferencesKey("email_address")
    private val STATUS_LABEL = stringPreferencesKey("status")


    // Signal/Flow for UI to observe
    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME] ?: "DefaultUser" }

    val phoneNumberFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PHONE_NUMBER] ?: "DefaultPhone"}

    val emailAddressFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[EMAIL_ADDRESS] ?: "DefaultEmail" }

    val statusLabel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[STATUS_LABEL] ?: "DefaultStatus" }


    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun updatePhoneNumber(phoneNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[PHONE_NUMBER] = phoneNumber
        }
    }

    suspend fun updateEmailAddress(emailAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL_ADDRESS] = emailAddress
        }
    }

    suspend fun updateStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[STATUS_LABEL] = status
        }
    }
}