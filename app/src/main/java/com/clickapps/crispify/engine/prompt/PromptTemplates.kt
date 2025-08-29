package com.clickapps.crispify.engine.prompt

import android.content.res.Resources
import androidx.annotation.RawRes
import com.clickapps.crispify.R

/**
 * PromptTemplates provides version-pinned prompt strings.
 * Source of truth is res/raw/prompt_template_v1.txt per PRD Appendix A.
 */
object PromptTemplates {
    private const val INPUT_PLACEHOLDER = "{{INPUT}}"

    fun loadLevelingTemplate(resources: Resources, @RawRes resId: Int = R.raw.prompt_template_v1): String {
        return resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }

    fun buildFromTemplate(template: String, input: String): String {
        return template.replace(INPUT_PLACEHOLDER, input)
    }
}
