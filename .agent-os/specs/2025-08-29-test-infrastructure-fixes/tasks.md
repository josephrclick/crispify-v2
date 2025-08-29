# Spec Tasks

These are the tasks to be completed for the spec detailed in @.agent-os/specs/2025-08-29-test-infrastructure-fixes/spec.md

> Created: 2025-08-29
> Status: Ready for Implementation

## Tasks

- [ ] 1. Migrate Coroutine Tests to runTest
  - [ ] 1.1 Write tests to verify runTest functionality
  - [ ] 1.2 Replace runBlocking with runTest in ProcessTextViewModelTokenLimitTest
  - [ ] 1.3 Replace runBlocking with runTest in ProcessTextViewModelBoundaryTest
  - [ ] 1.4 Replace arbitrary delay() calls with advanceUntilIdle()
  - [ ] 1.5 Update test imports to use kotlinx-coroutines-test
  - [ ] 1.6 Verify all timing-dependent tests pass

- [ ] 2. Fix Mockito-Kotlin Suspend Function Mocking
  - [ ] 2.1 Write tests for suspend function mocking patterns
  - [ ] 2.2 Update LlamaEngineTest to use onBlocking for suspend functions
  - [ ] 2.3 Fix FirstLaunchViewModelTest mock configuration
  - [ ] 2.4 Implement doAnswer patterns for TokenCallback interfaces
  - [ ] 2.5 Add proper argument matchers (eq, any) for null-safety
  - [ ] 2.6 Verify all Mockito-based tests pass

- [ ] 3. Implement Test-Specific DataStore
  - [ ] 3.1 Write tests for TestPreferencesManager
  - [ ] 3.2 Create TestPreferencesManager with in-memory storage
  - [ ] 3.3 Update test classes to use TestPreferencesManager
  - [ ] 3.4 Configure DataStore.create() with temporary files
  - [ ] 3.5 Verify Robolectric compatibility

- [ ] 4. Fix Compose UI Test Infrastructure
  - [ ] 4.1 Write tests for UI component visibility
  - [ ] 4.2 Add testTag modifiers to FirstLaunchScreen components
  - [ ] 4.3 Update FirstLaunchScreenTest with createComposeRule()
  - [ ] 4.4 Implement waitForIdle() before assertions
  - [ ] 4.5 Configure proper test semantics
  - [ ] 4.6 Verify all Compose UI tests pass

- [ ] 5. Implement Test Dispatcher Management
  - [ ] 5.1 Write tests for MainDispatcherRule
  - [ ] 5.2 Create MainDispatcherRule as TestWatcher
  - [ ] 5.3 Apply MainDispatcherRule to all coroutine test classes
  - [ ] 5.4 Ensure proper dispatcher cleanup
  - [ ] 5.5 Run full test suite and verify 100% pass rate