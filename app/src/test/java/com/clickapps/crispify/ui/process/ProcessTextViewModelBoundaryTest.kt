package com.clickapps.crispify.ui.process

import androidx.test.core.app.ApplicationProvider
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.engine.LlamaNativeLibrary
import com.clickapps.crispify.engine.TokenCounter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    override fun processText(inputText: String): String {
        processCalled = true
        return inputText
    }
    override fun releaseModel() {}
    override fun isModelLoaded(): Boolean = true
    override fun getMemoryUsage(): Long = 0
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessTextViewModelBoundaryTest {

    private fun vm(tokens: Int, native: LlamaNativeLibrary = CountingMockNativeLibrary()): Triple<ProcessTextViewModel, DiagnosticsManager, CountingMockNativeLibrary> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)
        val diagnosticsManager = DiagnosticsManager(preferencesManager.dataStore)
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
    fun withinLimit_1190_tokens_allowed() = runBlocking {
        val (vm, _, _) = vm(1190)
        vm.processText("abc")
        delay(50)
        // Should not set error
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun boundary_1200_tokens_allowed() = runBlocking {
        val (vm, _, _) = vm(TokenCounter.LIMIT_TOKENS)
        vm.processText("abc")
        delay(50)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun overLimit_1201_blocks_and_no_engine_call() = runBlocking {
        val native = CountingMockNativeLibrary()
        val (vm, _, lib) = vm(TokenCounter.LIMIT_TOKENS + 1, native)
        vm.processText("abc")
        delay(20)
        assertEquals("Please select a smaller amount of text for this version.", vm.uiState.value.error)
        assertTrue(!lib.processCalled)
    }
}
