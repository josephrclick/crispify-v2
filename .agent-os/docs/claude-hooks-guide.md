# Claude Code Hooks Configuration Guide

## Overview
This document describes the Claude Code hooks configured for the Crispify Android project. These hooks automate quality checks, enforce privacy requirements, and maintain code standards.

## Hook Configuration Location
- **Project hooks**: `.claude/settings.json` - Project-specific automation
- **Hook scripts**: `.claude/hooks/` - Executable scripts for complex validations

## Active Hooks

### 1. Kotlin/Java Compilation Check (PostToolUse)
**Trigger**: After editing or writing `.kt` or `.java` files
**Action**: Runs `./gradlew compileDebugKotlin` or `compileDebugJavaWithJavac`
**Purpose**: Immediately validates that code changes compile successfully

### 2. Resource Validation (PostToolUse)
**Trigger**: After editing XML resource files
**Action**: Runs `./gradlew processDebugResources`
**Purpose**: Ensures resource files are valid and properly formatted

### 3. AndroidManifest.xml Privacy Check (PostToolUse)
**Trigger**: After editing `AndroidManifest.xml`
**Action**: Runs `.claude/hooks/validate-manifest.sh`
**Purpose**: 
- Ensures NO `android.permission.INTERNET` is added (PRD requirement)
- Validates `ACTION_PROCESS_TEXT` intent filter exists
- Maintains absolute privacy guarantee

### 4. Pre-Commit Validation (PreToolUse)
**Trigger**: Before running `git commit` commands
**Action**: Runs `.claude/hooks/pre-commit-checks.sh`
**Purpose**:
- Prevents direct commits to main branch
- Validates Kotlin compilation
- Runs Android lint checks
- Checks privacy requirements
- Scans for debug code

### 5. Build Configuration Warning (PreToolUse)
**Trigger**: Before editing `build.gradle` files
**Action**: Shows warning about network dependencies
**Purpose**: Reminds about PRD requirement of no network access

### 6. Session Start (SessionStart)
**Trigger**: When Claude Code session begins
**Action**: 
- Shows PRD reminder
- Restarts Gradle daemon for optimal performance
**Purpose**: Ensures development environment is ready and requirements are clear

### 7. PRD Reminder (UserPromptSubmit)
**Trigger**: On every user prompt
**Action**: Brief reminder of core PRD requirements
**Purpose**: Keeps privacy and feature constraints top of mind

## Hook Scripts

### validate-manifest.sh
Validates AndroidManifest.xml files for:
- Absence of INTERNET permission (critical requirement)
- Presence of ACTION_PROCESS_TEXT intent filter
- Other privacy-related checks

### pre-commit-checks.sh
Comprehensive pre-commit validation:
1. Branch protection (no direct main commits)
2. Kotlin compilation check
3. Android lint validation
4. Privacy requirements verification
5. Debug code detection

### check-dependencies.sh
Scans for network-related dependencies:
- Checks for common networking libraries (Retrofit, OkHttp, etc.)
- Validates Play Asset Delivery configuration
- Ensures no analytics or tracking libraries

## Usage Notes

### For Developers
1. Hooks run automatically - no manual intervention needed
2. Failed hooks will block the associated action
3. Check hook output for specific error messages
4. Scripts in `.claude/hooks/` can be run manually for debugging

### For Agents
1. All hooks are configured to run automatically
2. Pay attention to hook output for validation errors
3. PRD reminders on each prompt help maintain focus
4. Pre-commit checks ensure code quality before commits

### Customization
To modify hooks:
1. Edit `.claude/settings.json` for hook configuration
2. Edit scripts in `.claude/hooks/` for validation logic
3. Test changes by triggering the associated events

## Troubleshooting

### Hook Not Running
- Ensure `.claude/settings.json` is properly formatted JSON
- Check that scripts in `.claude/hooks/` are executable (`chmod +x`)
- Verify matcher patterns match the actual tool events

### False Positives
- Review and adjust keyword lists in check scripts
- Consider context-specific exceptions
- Update validation logic as needed

### Performance Issues
- Add timeout values to long-running hooks
- Use `head` or `tail` to limit output
- Consider running expensive checks less frequently

## Benefits

1. **Automated Quality**: No need to manually run checks
2. **Privacy Protection**: Prevents accidental network permission additions
3. **Consistent Standards**: Every change follows the same validation
4. **Fast Feedback**: Issues caught immediately, not at build time
5. **PRD Compliance**: Automatic enforcement of requirements

## Future Enhancements

Consider adding:
- Automatic code formatting with ktlint/spotless
- Performance profiling for critical paths
- Memory usage validation
- Automated test execution for changed files
- Integration with GitHub Actions workflows