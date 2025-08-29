package com.clickapps.crispify.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Test-specific PreferencesManager using an isolated, in-memory-like DataStore
 * backed by a temporary file.
 */
class TestPreferencesManager(tempDir: File? = null) {

    companion object {
        val FIRST_LAUNCH_COMPLETE_KEY = booleanPreferencesKey("first_launch_complete")
        val DIAGNOSTICS_ENABLED_KEY = booleanPreferencesKey("diagnostics_enabled")
    }

    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            val dir = tempDir ?: File(System.getProperty("java.io.tmpdir"))
            dir.mkdirs()
            File(dir, "crispify_test_prefs_${System.nanoTime()}.preferences_pb").also { it.deleteOnExit() }
        }
    )

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        !(preferences[FIRST_LAUNCH_COMPLETE_KEY] ?: false)
    }

    val isDiagnosticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DIAGNOSTICS_ENABLED_KEY] ?: false
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_COMPLETE_KEY] = true
        }
    }

    suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIAGNOSTICS_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
