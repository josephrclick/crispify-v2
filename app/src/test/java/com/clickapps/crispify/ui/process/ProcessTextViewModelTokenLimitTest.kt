package com.clickapps.crispify.ui.process

import androidx.test.core.app.ApplicationProvider
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.engine.TokenCounter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeTokenCounter(private val tokens: Int) : TokenCounter {
    override fun count(text: String): Int = tokens
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessTextViewModelTokenLimitTest {
    @Test
    fun overLimit_showsExactPrdError() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)
        val diagnosticsManager = DiagnosticsManager(preferencesManager.dataStore)

        val viewModel = ProcessTextViewModel(
            llamaEngine = LlamaEngine(),
            tokenCounter = FakeTokenCounter(TokenCounter.LIMIT_TOKENS + 1),
            levelingTemplate = "### Simplified Text\n\nOriginal Text:\n{{INPUT}}",
            preferencesManager = preferencesManager,
            diagnosticsManager = diagnosticsManager
        )

        viewModel.processText("some input text")
        // Give coroutine a moment to update state
        delay(500)

        val state = viewModel.uiState.value
        assertEquals(
            "Please select a smaller amount of text for this version.",
            state.error
        )
    }
}
