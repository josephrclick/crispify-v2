package com.clickapps.crispify.testing

import android.app.Application

/**
 * Test application for Robolectric tests.
 * Provides a minimal application context for unit tests,
 * particularly for Compose UI tests that require an activity context.
 */
class TestApplication : Application()