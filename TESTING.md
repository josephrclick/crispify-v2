# Testing Guide for Crispify

This document provides guidance for running tests in the Crispify project.

## Prerequisites

### Required Java Version
- **Java 17** is required for running tests due to Robolectric compatibility
- The project is configured to automatically use Java 17 for tests via Gradle toolchain

### Test Environment Configuration

The project includes the following test environment configurations:

1. **gradle.properties**: Configures JVM settings and Java home
   - Sets Java 17 as the default JVM for Gradle
   - Allocates 4GB heap and 512MB metaspace for better performance

2. **robolectric.properties**: Configures Robolectric settings
   - Sets SDK version to 34 (maximum supported by Robolectric 4.11.1)
   - Located in `app/src/test/resources/`

3. **build.gradle.kts**: Configures Gradle toolchain
   - Enforces Java 17 for test execution
   - Provides detailed test output and logging

## Running Tests

### All Tests
```bash
# Stop any existing Gradle daemons to ensure clean environment
./gradlew --stop

# Run all unit tests
./gradlew test

# Run all unit tests with detailed output
./gradlew test --info
```

### Specific Test Classes
```bash
# Run a specific test class
./gradlew test --tests "com.clickapps.crispify.ProcessTextManifestTest"

# Run multiple specific test classes
./gradlew test --tests "com.clickapps.crispify.ProcessTextManifestTest" \
              --tests "com.clickapps.crispify.engine.prompt.PromptTemplatesTest"
```

### Debug vs Release Tests
```bash
# Run only debug variant tests
./gradlew testDebugUnitTest

# Run only release variant tests
./gradlew testReleaseUnitTest
```

### Instrumented Tests
```bash
# Run instrumented tests on connected device/emulator
./gradlew connectedAndroidTest

# Run specific instrumented test
./gradlew connectedAndroidTest --tests "com.clickapps.crispify.ProcessTextInstrumentedTest"
```

## Troubleshooting

### Java Version Issues

If you encounter errors like "Unsupported class file major version 67", this indicates Java version incompatibility:

1. Verify Java 17 is installed:
   ```bash
   ls /usr/lib/jvm/ | grep java-17
   ```

2. Install Java 17 if needed:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install openjdk-17-jdk
   
   # macOS with Homebrew
   brew install openjdk@17
   ```

3. Manually override Java version for a single test run:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew test
   ```

### SDK Version Issues

If you see errors about SDK version mismatches:
- The app targets SDK 36 but Robolectric 4.11.1 supports up to SDK 34
- This is handled by the `robolectric.properties` file
- Individual tests can override with `@Config(sdk = [34])` annotation if needed

### Mockito/DataStore Issues

Some tests may have issues with mocking DataStore Preferences:
- This is a known limitation with coroutines and suspend functions
- Consider using fake implementations instead of mocks for DataStore

### Test Failures After Clean Clone

After cloning the repository, ensure you:
1. Stop any existing Gradle daemons: `./gradlew --stop`
2. Clean the build: `./gradlew clean`
3. Run tests: `./gradlew test`

## Continuous Integration

For CI/CD pipelines, ensure:
1. Java 17 is available in the build environment
2. Set `JAVA_HOME` environment variable to Java 17 installation
3. Consider using Gradle build cache for faster builds
4. Run `./gradlew --stop` before test execution to ensure clean state

## Test Coverage

To generate test coverage reports:
```bash
# Enable coverage in build.gradle.kts first, then:
./gradlew testDebugUnitTestCoverage
```

Coverage reports will be available in:
`app/build/reports/coverage/test/debug/index.html`

## Best Practices

1. **Write tests with Java 17 compatibility in mind**
2. **Use @Config(sdk = [34]) for new Robolectric tests if needed**
3. **Prefer instrumented tests for UI and integration testing**
4. **Use unit tests for business logic and ViewModels**
5. **Mock external dependencies appropriately**
6. **Ensure tests are idempotent and don't depend on execution order**

## Known Issues

1. **Robolectric + Java 23**: Robolectric 4.11.1 doesn't support Java 23. Use Java 17.
2. **SDK 36 Support**: Robolectric doesn't yet support SDK 36. Tests run against SDK 34.
3. **DataStore Mocking**: Mocking DataStore with Mockito can be problematic due to coroutines.

## Getting Help

If you encounter test issues not covered here:
1. Check the build output for specific error messages
2. Review the GitHub Actions CI logs for similar issues
3. Consult the [Robolectric documentation](http://robolectric.org/)
4. Open an issue with detailed error messages and environment information