package com.clickapps.crispify.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RunTestSanityTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun virtual_time_advances_without_real_delay() = runTest {
        var completed = false
        launch(mainDispatcherRule.dispatcher) {
            delay(1_000)
            completed = true
        }

        assertFalse(completed)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(completed)
    }
}

