package com.clickapps.crispify.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.doAnswer
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for LlamaEngine model initialization
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LlamaEngineTest {
    
    @Mock
    private lateinit var mockNativeLibrary: LlamaNativeLibrary
    
    private lateinit var llamaEngine: LlamaEngine
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(false)
        llamaEngine = LlamaEngine(mockNativeLibrary)
    }
    
    @Test
    fun `initialize emits progress updates correctly`() = runTest {
        // Given
        val expectedProgress = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        
        `when`(mockNativeLibrary.loadModel(any(), any())).thenAnswer { invocation ->
            val progressCallback = invocation.getArgument<(Float) -> Unit>(1)
            expectedProgress.forEach { progress ->
                progressCallback(progress)
            }
            true
        }
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(true)
        
        // When
        val actualProgress = mutableListOf<Float>()
        val flow = llamaEngine.initialize { progress ->
            actualProgress.add(progress)
        }
        val flowProgress = flow.toList()
        
        // Then
        assertTrue(actualProgress.containsAll(expectedProgress))
        assertEquals(1.0f, flowProgress.last())
        verify(mockNativeLibrary).loadModel(any(), any())
    }
    
    @Test
    fun `initialize handles model loading failure`() = runTest {
        // Given
        `when`(mockNativeLibrary.loadModel(any(), any())).thenReturn(false)
        
        // When/Then
        assertFailsWith<ModelInitializationException> {
            llamaEngine.initialize {}.toList()
        }
    }
    
    @Test
    fun `initialize handles native library exception`() = runTest {
        // Given
        `when`(mockNativeLibrary.loadModel(any(), any()))
            .thenThrow(RuntimeException("Native library error"))
        
        // When/Then
        assertFailsWith<ModelInitializationException> {
            llamaEngine.initialize {}.toList()
        }
    }
    
    @Test
    fun `processText streams tokens when model is initialized`() = runTest {
        // Given - Initialize model first
        `when`(mockNativeLibrary.loadModel(any(), any())).thenReturn(true)
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(true)
        llamaEngine.initialize {}.toList()
        
        val inputText = "Complex technical documentation"
        val expectedTokens = listOf("Simple", " easy", " to", " understand", " text")
        val receivedTokens = mutableListOf<String>()
        
        // Mock the processText to call the callback with tokens
        doAnswer { invocation ->
            val callback = invocation.getArgument<TokenCallback>(1)
            expectedTokens.forEach { token ->
                callback.onToken(token, false)
            }
            callback.onToken("", true)
            null
        }.`when`(mockNativeLibrary).processText(eq(inputText), any())
        
        // When
        llamaEngine.processText(inputText) { token, isFinished ->
            if (!isFinished) {
                receivedTokens.add(token)
            }
        }
        
        // Then
        assertEquals(expectedTokens, receivedTokens)
        verify(mockNativeLibrary).processText(eq(inputText), any())
    }
    
    @Test
    fun `processText handles empty input`() = runTest {
        // Given - Initialize model first
        `when`(mockNativeLibrary.loadModel(any(), any())).thenReturn(true)
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(true)
        llamaEngine.initialize {}.toList()
        
        val inputText = ""
        val receivedTokens = mutableListOf<String>()
        
        // Mock empty response
        doAnswer { invocation ->
            val callback = invocation.getArgument<TokenCallback>(1)
            callback.onToken("", true)
            null
        }.`when`(mockNativeLibrary).processText(eq(inputText), any())
        
        // When
        llamaEngine.processText(inputText) { token, isFinished ->
            if (!isFinished) {
                receivedTokens.add(token)
            }
        }
        
        // Then
        assertTrue(receivedTokens.isEmpty())
    }
    
    @Test
    fun `isInitialized returns correct state`() = runTest {
        // Initially not initialized
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(false)
        assertEquals(false, llamaEngine.isInitialized())
        
        // After successful initialization
        `when`(mockNativeLibrary.loadModel(any(), any())).thenReturn(true)
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(true)
        llamaEngine.initialize {}.toList()
        
        assertEquals(true, llamaEngine.isInitialized())
    }
    
    @Test
    fun `release cleans up resources`() = runTest {
        // Given
        `when`(mockNativeLibrary.loadModel(any(), any())).thenReturn(true)
        `when`(mockNativeLibrary.isModelLoaded()).thenReturn(true)
        llamaEngine.initialize {}.toList()
        
        // When
        llamaEngine.release()
        
        // Then
        verify(mockNativeLibrary).releaseModel()
        assertEquals(false, llamaEngine.isInitialized())
    }
}