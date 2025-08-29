# Spec Tasks

These are the tasks to be completed for the spec detailed in @.agent-os/specs/2025-08-29-test-infrastructure-fixes/spec.md

> Created: 2025-08-29
> Status: Ready for Implementation

## Tasks

- [x] 1. Migrate Coroutine Tests to runTest
  - [x] 1.1 Write tests to verify runTest functionality
  - [x] 1.2 Replace runBlocking with runTest in ProcessTextViewModelTokenLimitTest
  - [x] 1.3 Replace runBlocking with runTest in ProcessTextViewModelBoundaryTest
  - [x] 1.4 Replace arbitrary delay() calls with advanceUntilIdle()
  - [x] 1.5 Update test imports to use kotlinx-coroutines-test
  - [x] 1.6 Verify all timing-dependent tests pass

- [x] 2. Fix Mockito-Kotlin Suspend Function Mocking
  - [x] 2.1 Write tests for suspend function mocking patterns
  - [x] 2.2 Update LlamaEngineTest to use onBlocking for suspend functions
  - [x] 2.3 Fix FirstLaunchViewModelTest mock configuration
  - [x] 2.4 Implement doAnswer patterns for TokenCallback interfaces
  - [x] 2.5 Add proper argument matchers (eq, any) for null-safety
  - [x] 2.6 Verify all Mockito-based tests pass

- [x] 3. Implement Test-Specific DataStore
  - [x] 3.1 Write tests for TestPreferencesManager
  - [x] 3.2 Create TestPreferencesManager with in-memory storage
  - [x] 3.3 Update test classes to use TestPreferencesManager
  - [x] 3.4 Configure DataStore.create() with temporary files
  - [x] 3.5 Verify Robolectric compatibility

- [x] 4. Fix Compose UI Test Infrastructure
  - [x] 4.1 Write tests for UI component visibility
  - [x] 4.2 Add testTag modifiers to FirstLaunchScreen components
  - [x] 4.3 Update FirstLaunchScreenTest with createComposeRule()
  - [x] 4.4 Implement waitForIdle() before assertions
  - [x] 4.5 Configure proper test semantics
  - [x] 4.6 Verify all Compose UI tests pass

- [x] 5. Implement Test Dispatcher Management
  - [x] 5.1 Write tests for MainDispatcherRule
  - [x] 5.2 Create MainDispatcherRule as TestWatcher
  - [x] 5.3 Apply MainDispatcherRule to all coroutine test classes
  - [x] 5.4 Ensure proper dispatcher cleanup
  - [x] 5.5 Run full test suite and verify 100% pass rate
