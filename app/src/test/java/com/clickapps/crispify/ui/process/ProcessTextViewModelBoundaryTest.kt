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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeTokenCounterBoundary(private val tokens: Int) : TokenCounter {
    override fun count(text: String): Int = tokens
}

private class CountingMockNativeLibrary : LlamaNativeLibrary {
    var processCalled = false
    override fun loadModel(modelPath: String, progressCallback: (Float) -> Unit): Boolean = true
    override fun processText(inputText: String, tokenCallback: TokenCallback) {
        processCalled = true
        // Simple token streaming simulation
        inputText.split(" ").forEach { word ->
            tokenCallback.onToken(word, false)
        }
        tokenCallback.onToken("", true)
    }
    override fun cancelProcessing() {}
    override fun releaseModel() {}
    override fun isModelLoaded(): Boolean = true
    override fun getMemoryUsage(): Long = 0
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProcessTextViewModelBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vm(tokens: Int, native: LlamaNativeLibrary = CountingMockNativeLibrary()): Triple<ProcessTextViewModel, DiagnosticsManager, CountingMockNativeLibrary> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)
        val testPrefs = TestPreferencesManager()
        val diagnosticsManager = DiagnosticsManager(testPrefs.dataStore)
        val nativeLib = native as CountingMockNativeLibrary
        val viewModel = ProcessTextViewModel(
            llamaEngine = LlamaEngine(nativeLib),
            tokenCounter = FakeTokenCounterBoundary(tokens),
            levelingTemplate = "### Simplified Text\n\nOriginal Text:\n{{INPUT}}",
            preferencesManager = preferencesManager,
            diagnosticsManager = diagnosticsManager
        )
        return Triple(viewModel, diagnosticsManager, nativeLib)
    }

    @Test
    fun withinLimit_1190_tokens_allowed() = runTest {
        val (vm, _, _) = vm(1190)
        vm.processText("abc")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        // Should not set error
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun boundary_1200_tokens_allowed() = runTest {
        val (vm, _, _) = vm(TokenCounter.LIMIT_TOKENS)
        vm.processText("abc")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun overLimit_1201_blocks_and_no_engine_call() = runTest {
        val native = CountingMockNativeLibrary()
        val (vm, _, lib) = vm(TokenCounter.LIMIT_TOKENS + 1, native)
        vm.processText("abc")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Please select a smaller amount of text for this version.", vm.uiState.value.error)
        assertTrue(!lib.processCalled)
    }
}
