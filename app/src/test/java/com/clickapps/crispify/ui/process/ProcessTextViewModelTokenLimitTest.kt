package com.clickapps.crispify.ui.process

import androidx.test.core.app.ApplicationProvider
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.engine.LlamaNativeLibrary
import com.clickapps.crispify.engine.TokenCallback
import com.clickapps.crispify.engine.TokenCounter
import com.clickapps.crispify.testing.TestPreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import com.clickapps.crispify.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeTokenCounter(private val tokens: Int) : TokenCounter {
    override fun count(text: String): Int = tokens
}

private class LoadedNoOpNativeLibrary : LlamaNativeLibrary {
    override fun loadModel(modelPath: String, progressCallback: (Float) -> Unit): Boolean = true
    override fun processText(inputText: String, tokenCallback: TokenCallback) { tokenCallback.onToken("", true) }
    override fun cancelProcessing() {}
    override fun releaseModel() {}
    override fun isModelLoaded(): Boolean = true
    override fun getMemoryUsage(): Long = 0
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProcessTextViewModelTokenLimitTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    @Test
    fun overLimit_showsExactPrdError() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)
        val testPrefs = TestPreferencesManager()
        val diagnosticsManager = DiagnosticsManager(testPrefs.dataStore)

        val viewModel = ProcessTextViewModel(
            llamaEngine = LlamaEngine(context),
            tokenCounter = FakeTokenCounter(TokenCounter.LIMIT_TOKENS + 1),
            levelingTemplate = "### Simplified Text\n\nOriginal Text:\n{{INPUT}}",
            preferencesManager = preferencesManager,
            diagnosticsManager = diagnosticsManager
        )

        viewModel.processText("some input text")
        // Advance Main dispatcher tasks until idle
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Please select a smaller amount of text for this version.",
            state.error
        )
    }
}
