package com.clickapps.crispify.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for FirstLaunchViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstLaunchViewModelTest {

    @get:Rule
    val mainDispatcherRule = com.clickapps.crispify.testing.MainDispatcherRule()
    
    @Mock
    private lateinit var mockDataStore: DataStore<Preferences>
    
    @Mock
    private lateinit var mockModelInitializer: ModelInitializer
    
    private lateinit var viewModel: FirstLaunchViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun `initial state has model loading true`() = runTest {
        // Given
        val mockPreferences = MutableStateFlow(mock(Preferences::class.java))
        `when`(mockDataStore.data).thenReturn(mockPreferences)
        `when`(mockModelInitializer.initialize(any())).thenReturn(flow { emit(0.5f) })
        
        // When
        viewModel = FirstLaunchViewModel(mockDataStore, mockModelInitializer, mainDispatcherRule.dispatcher)
        
        // Then
        assertTrue(viewModel.uiState.value.isModelLoading)
        assertEquals(0f, viewModel.uiState.value.modelLoadingProgress)
    }
    
    @Test
    fun `model initialization updates progress`() = runTest {
        // Given
        val mockPreferences = MutableStateFlow(mock(Preferences::class.java))
        `when`(mockDataStore.data).thenReturn(mockPreferences)
        
        val progressFlow = flow {
            emit(0.25f)
            emit(0.5f)
            emit(0.75f)
            emit(1.0f)
        }
        `when`(mockModelInitializer.initialize(any())).thenReturn(progressFlow)
        
        // When
        viewModel = FirstLaunchViewModel(mockDataStore, mockModelInitializer, mainDispatcherRule.dispatcher)
        val states = mutableListOf<FirstLaunchUiState>()
        val job = viewModel.uiState.onEach { states.add(it) }.launchIn(this)
        
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        
        // Then (ensure completion state observed)
        assertTrue(states.last().modelLoadingProgress == 1.0f)
        assertFalse(states.last().isModelLoading)
        
        job.cancel()
    }
    
    @Test
    fun `toggleDiagnostics changes state and persists`() = runTest {
        // Given
        val mockPreferences = MutableStateFlow(mock(Preferences::class.java))
        `when`(mockDataStore.data).thenReturn(mockPreferences)
        `when`(mockModelInitializer.initialize(any())).thenReturn(flowOf(1.0f))
        
        viewModel = FirstLaunchViewModel(mockDataStore, mockModelInitializer, mainDispatcherRule.dispatcher)
        
        // When
        viewModel.toggleDiagnostics(true)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.isDiagnosticsEnabled)
        verify(mockDataStore).edit(any())
    }
    
    @Test
    fun `onDismiss sets first launch complete flag`() = runTest {
        // Given
        val mockPreferences = MutableStateFlow(mock(Preferences::class.java))
        `when`(mockDataStore.data).thenReturn(mockPreferences)
        `when`(mockModelInitializer.initialize(any())).thenReturn(flowOf(1.0f))
        
        viewModel = FirstLaunchViewModel(mockDataStore, mockModelInitializer, mainDispatcherRule.dispatcher)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.onDismiss()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(mockDataStore).edit(any())
    }
    
    @Test
    fun `model initialization error is handled gracefully`() = runTest {
        // Given
        val mockPreferences = MutableStateFlow(mock(Preferences::class.java))
        `when`(mockDataStore.data).thenReturn(mockPreferences)
        
        val errorFlow = flow<Float> {
            emit(0.5f)
            throw Exception("Model initialization failed")
        }
        `when`(mockModelInitializer.initialize(any())).thenReturn(errorFlow)
        
        // When
        viewModel = FirstLaunchViewModel(mockDataStore, mockModelInitializer, mainDispatcherRule.dispatcher)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.hasError)
        assertFalse(viewModel.uiState.value.isModelLoading)
    }
    
    @Test
    fun `diagnostics preference is loaded from storage`() = runTest {
        // Given
        val mockPreferences = mock(Preferences::class.java)
        `when`(mockPreferences[booleanPreferencesKey("diagnostics_enabled")]).thenReturn(true)
        
        val preferencesFlow = MutableStateFlow(mockPreferences)
        `when`(mockDataStore.data).thenReturn(preferencesFlow)
        `when`(mockModelInitializer.initialize(any())).thenReturn(flowOf(1.0f))
        
        // When
        viewModel = FirstLaunchViewModel(mockDataStore, mockModelInitializer, mainDispatcherRule.dispatcher)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.isDiagnosticsEnabled)
    }
}
