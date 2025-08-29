package com.clickapps.crispify

import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessTextActivityIntentTest {
    @Test
    fun launch_withProcessTextExtras_doesNotCrash() {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "Hello world")
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            type = "text/plain"
        }
        val controller = Robolectric.buildActivity(ProcessTextActivity::class.java, intent)
        controller.setup().pause().stop().destroy()
    }
}

