package com.clickapps.crispify.ui.process

import androidx.test.core.app.ApplicationProvider
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.engine.TokenCounter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import com.clickapps.crispify.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProcessTextViewModelConcurrencyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeTokenCounterAlways(private val value: Int = 10) : TokenCounter {
        override fun count(text: String): Int = value
    }

    private fun vm(engine: LlamaEngine): ProcessTextViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesManager = PreferencesManager(context)
        val diagnosticsManager = DiagnosticsManager(preferencesManager.dataStore)
        return ProcessTextViewModel(
            llamaEngine = LlamaEngine(context, native),
            tokenCounter = FakeTokenCounterAlways(10), // within limit
            levelingTemplate = "### Simplified Text\n\nOriginal Text:\n{{INPUT}}",
            preferencesManager = preferencesManager,
            diagnosticsManager = diagnosticsManager
        )
    }

    @Test
    fun latestCallWins_whenCalledRapidly() = runTest {
        val engine = mock<LlamaEngine>()
        whenever(engine.isInitialized()).thenReturn(true)
        var lastInput: String? = null
        doAnswer { invocation ->
            val input = invocation.getArgument<String>(0)
            val cb = invocation.getArgument<(String, Boolean) -> Unit>(1)
            lastInput = input
            cb("TAG:$input", false)
            cb("", true)
            null
        }.whenever(engine).processText(any(), any())
        val vm = vm(engine)
        // First call; allow initialization to complete
        vm.processText("first")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        // Second call should supersede the first
        vm.processText("second")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        // Ensure latest engine call used second input
        assertEquals(true, lastInput?.contains("second") == true)
        assertEquals(null, state.error)

        // Cleanup to avoid pending Main dispatchers
        vm.cancelProcessing()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    }
}
