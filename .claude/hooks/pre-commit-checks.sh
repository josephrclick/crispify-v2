#!/bin/bash

echo "🔍 Running pre-commit checks for Crispify..."

# Check current branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ]; then
    echo "❌ ERROR: Direct commits to main branch are not allowed!"
    echo "   Please create a feature branch first:"
    echo "   git checkout -b feature/your-feature-name"
    exit 1
fi

echo "✅ Branch check passed (on $CURRENT_BRANCH)"

# Run compilation check
echo "📦 Checking Kotlin compilation..."
if ./gradlew compileDebugKotlin --daemon --console=plain > /dev/null 2>&1; then
    echo "✅ Kotlin compilation successful"
else
    echo "❌ Kotlin compilation failed. Please fix errors before committing."
    exit 1
fi

# Run lint check (non-blocking, just warnings)
echo "🧹 Running Android lint..."
if ./gradlew lint --daemon --console=plain > /dev/null 2>&1; then
    echo "✅ Lint check passed"
else
    echo "⚠️  Warning: Lint check found issues (commit allowed, but please review)"
fi

# Validate manifest
echo "🔒 Validating privacy requirements..."
if [ -f ".claude/hooks/validate-manifest.sh" ]; then
    if ./.claude/hooks/validate-manifest.sh > /dev/null 2>&1; then
        echo "✅ Privacy validation passed"
    else
        echo "❌ Privacy validation failed. Check AndroidManifest.xml"
        exit 1
    fi
fi

# Check for debugging code
echo "🔍 Checking for debug code..."
if grep -r "Log\.d\|Log\.v\|println\|System\.out" app/src/main/java --include="*.kt" --include="*.java" 2>/dev/null | grep -v "^Binary file" > /dev/null; then
    echo "⚠️  Warning: Found debug logging statements (non-blocking)"
fi

echo "✅ All pre-commit checks completed successfully!"