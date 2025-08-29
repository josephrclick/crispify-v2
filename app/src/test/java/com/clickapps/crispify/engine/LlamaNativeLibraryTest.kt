package com.clickapps.crispify.engine

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for LlamaNativeLibrary implementations
 */
class LlamaNativeLibraryTest {
    
    @Test
    fun `loadModel should return true for mock implementation`() {
        val library = MockLlamaNativeLibrary()
        var progressCount = 0
        
        val result = library.loadModel("test_model.gguf") { progress ->
            progressCount++
            assertTrue("Progress should be between 0 and 1", progress in 0f..1f)
        }
        
        assertTrue("Model should load successfully", result)
        assertTrue("Progress should be called multiple times", progressCount > 0)
        assertTrue("Model should be marked as loaded", library.isModelLoaded())
    }
    
    @Test
    fun `processText should stream tokens with mock implementation`() {
        val library = MockLlamaNativeLibrary()
        library.loadModel("test_model.gguf") { }
        
        val tokens = mutableListOf<String>()
        var isFinishedCalled = false
        val latch = CountDownLatch(1)
        
        library.processText("utilize implement functionality") { token, isFinished ->
            if (isFinished) {
                isFinishedCalled = true
                latch.countDown()
            } else {
                tokens.add(token)
            }
        }
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should have received tokens", tokens.isNotEmpty())
        assertTrue("Should have called isFinished", isFinishedCalled)
        
        val result = tokens.joinToString("")
        assertTrue("Should contain simplified text", result.contains("use"))
    }
    
    @Test
    fun `processText should throw when model not loaded`() {
        val library = MockLlamaNativeLibrary()
        
        try {
            library.processText("test") { _, _ -> }
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Model not loaded", e.message)
        }
    }
    
    @Test
    fun `cancelProcessing should stop token generation`() {
        val library = MockLlamaNativeLibrary()
        library.loadModel("test_model.gguf") { }
        
        val tokens = mutableListOf<String>()
        val latch = CountDownLatch(1)
        
        Thread {
            library.processText("This is a long text that should be cancelled") { token, isFinished ->
                if (isFinished) {
                    latch.countDown()
                } else {
                    tokens.add(token)
                    // Cancel after first token
                    if (tokens.size == 1) {
                        library.cancelProcessing()
                    }
                }
            }
        }.start()
        
        assertTrue("Should complete within timeout", latch.await(2, TimeUnit.SECONDS))
        assertTrue("Should have received at least one token before cancel", tokens.size >= 1)
        assertTrue("Should not have processed all tokens", tokens.size < 10)
    }
    
    @Test
    fun `getMemoryUsage should return 0 when model not loaded`() {
        val library = MockLlamaNativeLibrary()
        assertEquals(0L, library.getMemoryUsage())
    }
    
    @Test
    fun `getMemoryUsage should return non-zero when model loaded`() {
        val library = MockLlamaNativeLibrary()
        library.loadModel("test_model.gguf") { }
        
        val usage = library.getMemoryUsage()
        assertTrue("Memory usage should be positive", usage > 0)
        
        // Verify it's a reasonable value (100MB for mock)
        val expectedMB = 100
        val actualMB = usage / (1024 * 1024)
        assertEquals(expectedMB.toLong(), actualMB)
    }
    
    @Test
    fun `releaseModel should free resources`() {
        val library = MockLlamaNativeLibrary()
        library.loadModel("test_model.gguf") { }
        
        assertTrue("Model should be loaded", library.isModelLoaded())
        assertTrue("Memory should be in use", library.getMemoryUsage() > 0)
        
        library.releaseModel()
        
        assertFalse("Model should not be loaded", library.isModelLoaded())
        assertEquals("Memory should be freed", 0L, library.getMemoryUsage())
    }
    
    @Test
    fun `TokenCallback interface should work as SAM`() {
        // Test that TokenCallback can be used as a SAM interface
        val callback: TokenCallback = TokenCallback { token, isFinished ->
            assertNotNull(token)
            assertNotNull(isFinished)
        }
        
        // Invoke the callback
        callback.onToken("test", false)
    }
}