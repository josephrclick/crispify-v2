package com.clickapps.crispify.engine.prompt

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PromptTemplatesTest {
    @Test
    fun loadAndBuild_replacesPlaceholder_andContainsSections() {
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        val template = PromptTemplates.loadLevelingTemplate(resources)
        val input = "This is a Test."
        val prompt = PromptTemplates.buildFromTemplate(template, input)

        assertTrue(prompt.contains("### Simplified Text"))
        assertTrue(prompt.contains("System Preface (internal):"))
        assertTrue(prompt.contains(input))
        assertFalse(prompt.contains("{{INPUT}}"))
    }
}
