package com.clickapps.crispify.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for FirstLaunchScreen composable
 * Tests UI components, interactions, and state changes
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], instrumentedPackages = ["androidx.loader.content"])
class FirstLaunchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun firstLaunchScreen_displaysAllComponents() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = true,
                modelLoadingProgress = 0.5f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then - verify all expected UI components are present
        composeTestRule.onNodeWithTag("BrandingPlaceholder").assertExists()
        composeTestRule.onNodeWithText("Crispify", substring = true).assertExists()
        composeTestRule.onNodeWithText("Select text in any app", substring = true).assertExists()
        composeTestRule.onNodeWithTag("ModelLoadingProgress").assertExists()
        composeTestRule.onNodeWithText("Local Diagnostics", substring = true).assertExists()
        composeTestRule.onNodeWithText("Get Started").assertExists()
    }

    @Test
    fun dismissButton_isDisabledWhileLoading() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = true,
                modelLoadingProgress = 0.3f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Get Started")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun dismissButton_isEnabledAfterLoading() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = false,
                modelLoadingProgress = 1.0f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Get Started")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun diagnosticsSwitch_togglesCorrectly() {
        // Given
        var diagnosticsEnabled = false
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = true,
                modelLoadingProgress = 0.5f,
                isDiagnosticsEnabled = diagnosticsEnabled,
                onDiagnosticsToggle = { diagnosticsEnabled = it },
                onDismiss = {}
            )
        }

        // When
        composeTestRule.onNodeWithTag("DiagnosticsSwitch")
            .assertExists()
            .performClick()

        // Then
        assert(diagnosticsEnabled)
    }

    @Test
    fun progressIndicator_showsWhileLoading() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = true,
                modelLoadingProgress = 0.7f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then
        composeTestRule.onNodeWithTag("ModelLoadingProgress")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun progressIndicator_hidesAfterLoading() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = false,
                modelLoadingProgress = 1.0f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then
        composeTestRule.onNodeWithTag("ModelLoadingProgress")
            .assertDoesNotExist()
    }

    @Test
    fun dismissButton_callsOnDismissCallback() {
        // Given
        var dismissed = false
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = false,
                modelLoadingProgress = 1.0f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = { dismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Get Started")
            .performClick()

        // Then
        assert(dismissed)
    }

    @Test
    fun instructionText_displaysCorrectContent() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = true,
                modelLoadingProgress = 0.5f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then - verify instruction text content
        composeTestRule.onNodeWithText(
            "Select text in any app and find 'Crispify' in the selection menu", 
            substring = true
        ).assertExists()
    }

    @Test
    fun modelLoadingText_displaysWhileLoading() {
        // Given
        composeTestRule.setContent {
            FirstLaunchScreen(
                isModelLoading = true,
                modelLoadingProgress = 0.5f,
                isDiagnosticsEnabled = false,
                onDiagnosticsToggle = {},
                onDismiss = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Preparing AI model...", substring = true)
            .assertExists()
    }
}