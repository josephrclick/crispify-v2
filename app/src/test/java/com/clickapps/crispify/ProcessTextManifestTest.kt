package com.clickapps.crispify

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric test to validate ACTION_PROCESS_TEXT manifest configuration
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessTextManifestTest {

    @Test
    fun resolveProcessTextIntent_resolvesToProcessTextActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pm = context.packageManager

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
        }

        val resolveInfo = pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        assertNotNull("ACTION_PROCESS_TEXT should resolve", resolveInfo)

        val activityInfo = resolveInfo!!.activityInfo
        assertEquals(
            "Activity class should be ProcessTextActivity",
            ComponentName(context, ProcessTextActivity::class.java).className,
            ComponentName(activityInfo.packageName, activityInfo.name).className
        )

        // Exported for system surface (PROCESS_TEXT chooser)
        assertTrue("ProcessTextActivity must be exported", activityInfo.exported)
    }
}
