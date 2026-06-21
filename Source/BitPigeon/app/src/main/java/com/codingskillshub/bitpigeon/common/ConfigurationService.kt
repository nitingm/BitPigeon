package com.codingskillshub.bitpigeon.common

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
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
    private val USER_ID_KEY = stringPreferencesKey("permanent_user_id")
    private val APP_THEME = stringPreferencesKey("app_theme")

    // Signal/Flow for UI to observe
    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME] ?: "DefaultUser" }

    val phoneNumberFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PHONE_NUMBER] ?: "DefaultPhone"}

    val emailAddressFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[EMAIL_ADDRESS] ?: "DefaultEmail" }

    val statusLabel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[STATUS_LABEL] ?: "DefaultStatus" }

    val userIdFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_ID_KEY] }

    val appThemeFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[APP_THEME] }

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

    suspend fun changeAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    suspend fun generateAndSaveUserId() {
        // Option A: Simple UUID (Unique but not cryptographically verifiable)
        context.dataStore.edit { preferences ->
            val existingId = preferences[USER_ID_KEY]
            if (existingId == null) {
                // Generate only if it does not exist
                val newId = UUID.randomUUID().toString()
                preferences[USER_ID_KEY] = newId
                Log.d("ConfigurationService", "Generated new Unique P2P ID: $newId")
            }

        }
        Log.d("ConfigurationService", "My Unique P2P ID is: ${userIdFlow.firstOrNull()}")
    }

}