package com.clickapps.crispify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property to create a DataStore instance
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "crispify_preferences")

/**
 * Manager class for handling app preferences using DataStore
 */
class PreferencesManager(private val context: Context) {
    
    companion object {
        val FIRST_LAUNCH_COMPLETE_KEY = booleanPreferencesKey("first_launch_complete")
        val DIAGNOSTICS_ENABLED_KEY = booleanPreferencesKey("diagnostics_enabled")
    }
    
    val dataStore = context.dataStore
    
    /**
     * Check if this is the first launch of the app
     */
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        !(preferences[FIRST_LAUNCH_COMPLETE_KEY] ?: false)
    }
    
    /**
     * Get the current diagnostics preference
     */
    val isDiagnosticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DIAGNOSTICS_ENABLED_KEY] ?: false
    }
    
    /**
     * Mark first launch as complete
     */
    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_COMPLETE_KEY] = true
        }
    }
    
    /**
     * Update diagnostics preference
     */
    suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIAGNOSTICS_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Clear all preferences (useful for testing)
     */
    suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}