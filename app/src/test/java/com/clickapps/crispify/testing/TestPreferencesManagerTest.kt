package com.clickapps.crispify.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TestPreferencesManagerTest {

    @Test
    fun defaults_and_updates_work() = runTest {
        val prefs = TestPreferencesManager()

        // Defaults
        assertTrue(prefs.isFirstLaunch.first())
        assertFalse(prefs.isDiagnosticsEnabled.first())

        // Updates
        prefs.setFirstLaunchComplete()
        prefs.setDiagnosticsEnabled(true)

        assertFalse(prefs.isFirstLaunch.first())
        assertTrue(prefs.isDiagnosticsEnabled.first())

        // Clear
        prefs.clearPreferences()
        assertTrue(prefs.isFirstLaunch.first())
        assertFalse(prefs.isDiagnosticsEnabled.first())
    }
}

