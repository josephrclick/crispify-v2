package com.clickapps.crispify

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProcessTextInstrumentedTest {

    @get:Rule
    val activityRule = ActivityTestRule(ProcessTextActivity::class.java, true, false)

    @Test
    fun launch_withProcessTextIntent_startsActivity() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            setPackage(ctx.packageName)
            putExtra(Intent.EXTRA_PROCESS_TEXT, "Hello from instrumented test")
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            type = "text/plain"
        }
        activityRule.launchActivity(intent)
        // Basic sanity: activity launched without crash
    }
}

