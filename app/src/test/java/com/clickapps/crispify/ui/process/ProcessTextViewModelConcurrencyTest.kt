package com.clickapps.crispify.ui.process

import androidx.test.core.app.ApplicationProvider
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.engine.LlamaNativeLibrary
import com.clickapps.crispify.engine.TokenCallback
import com.clickapps.crispify.engine.TokenCounter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FastNativeLibrary(private val tag: String) : LlamaNativeLibrary {
    override fun loadModel(modelPath: String, progressCallback: (Float) -> Unit): Boolean = true
    override fun processText(inputText: String, tokenCallback: TokenCallback) {
        // Return a quick, distinctive result via streaming
        val result = "$tag:$inputText"
        tokenCallback.onToken(result, false)
        tokenCallback.onToken("", true)
    }
    override fun cancelProcessing() {}
    override fun releaseModel() {}
    override fun isModelLoaded(): Boolean = true
    override fun getMemoryUsage(): Long = 0
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessTextViewModelConcurrencyTest {

    private class FakeTokenCounterAlways(private val value: Int = 10) : TokenCounter {
        override fun count(text: String): Int = value
    }

    private fun vm(native: LlamaNativeLibrary): ProcessTextViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)
        val diagnosticsManager = DiagnosticsManager(preferencesManager.dataStore)
        return ProcessTextViewModel(
            llamaEngine = LlamaEngine(native),
            tokenCounter = FakeTokenCounterAlways(10), // within limit
            levelingTemplate = "### Simplified Text\n\nOriginal Text:\n{{INPUT}}",
            preferencesManager = preferencesManager,
            diagnosticsManager = diagnosticsManager
        )
    }

    @Test
    fun latestCallWins_whenCalledRapidly() = runBlocking {
        val vm = vm(FastNativeLibrary("TAG"))
        vm.processText("first")
        // Rapidly call again to cancel previous job
        vm.processText("second")
        // Allow pseudo-streaming to append a bit
        delay(40)
        val state = vm.uiState.value
        // Ensure result corresponds to latest input
        // Output includes tag prefix; we just check it contains latest input
        assertEquals(true, state.processedText.contains("second"))
        assertEquals(null, state.error)
    }
}
