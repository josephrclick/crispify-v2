# Technical Specification

This is the technical specification for the spec detailed in @.agent-os/specs/2025-08-29-test-infrastructure-fixes/spec.md

> Created: 2025-08-29
> Version: 1.0.0

## Technical Requirements

### Coroutine Testing Migration
- Replace all `runBlocking` calls with `runTest` from `kotlinx-coroutines-test`
- Use `advanceUntilIdle()` instead of arbitrary `delay()` calls
- Implement `StandardTestDispatcher` for controlled virtual time advancement
- Configure `TestScope` with proper exception handling

### Mockito-Kotlin Configuration
- Use `onBlocking` for stubbing suspend functions
- Implement proper `doAnswer` patterns for TokenCallback interfaces
- Configure argument matchers with `eq()` and `any()` for Kotlin null-safety
- Create helper functions for common mock patterns

### DataStore Test Implementation
- Create `TestPreferencesManager` with in-memory storage
- Use `DataStore.create()` with temporary file for tests
- Implement proper serializer for test environment
- Ensure thread-safe access in concurrent tests

### Compose UI Test Setup
- Add `testTag` modifiers to all testable UI components
- Use `createComposeRule()` for UI test setup
- Implement `waitForIdle()` before assertions
- Configure proper test semantics for accessibility

### Test Dispatcher Management
- Create `MainDispatcherRule` as JUnit TestWatcher
- Use `Dispatchers.setMain()` and `resetMain()` properly
- Ensure consistent dispatcher across all test classes
- Handle dispatcher cleanup in @After methods

## Approach

The implementation will be performed in phases to minimize disruption to the existing codebase:

### Phase 1: Core Infrastructure
1. Add missing test dependencies to `build.gradle.kts`
2. Create `MainDispatcherRule` for consistent test dispatcher management
3. Implement base test classes with common setup patterns

### Phase 2: Coroutine Testing Migration
1. Replace `runBlocking` with `runTest` in all test files
2. Remove arbitrary `delay()` calls and use `advanceUntilIdle()`
3. Configure proper test scopes and dispatchers

### Phase 3: Mock Configuration Updates
1. Update all suspend function mocking to use `onBlocking`
2. Fix argument matchers for Kotlin null-safety
3. Create helper functions for common mocking patterns

### Phase 4: DataStore Testing
1. Implement `TestPreferencesManager` with in-memory storage
2. Update all DataStore-related tests to use the test implementation
3. Ensure proper cleanup between test cases

### Phase 5: UI Test Enhancement
1. Add `testTag` modifiers to UI components
2. Update Compose test setup with proper rules
3. Implement proper wait strategies for UI assertions

## External Dependencies

- **kotlinx-coroutines-test:1.7.3** - Required for runTest and virtual time control
- **Justification:** Provides deterministic testing for coroutines with virtual time
- **mockito-kotlin:5.1.0** - Enhanced Kotlin support for Mockito
- **Justification:** Fixes null-safety issues and provides onBlocking for suspend functions