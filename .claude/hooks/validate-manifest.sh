#!/bin/bash

# Validate AndroidManifest.xml for privacy requirements
# Per PRD: Must NOT include android.permission.INTERNET

MANIFEST_FILES=$(find . -name "AndroidManifest.xml" -not -path "./build/*" -not -path "./app/src/main/cpp/llama.cpp/*" 2>/dev/null)

if [ -z "$MANIFEST_FILES" ]; then
    echo "⚠️  No AndroidManifest.xml files found"
    exit 0
fi

for manifest in $MANIFEST_FILES; do
    echo "🔍 Checking $manifest for privacy compliance..."
    
    # Check for INTERNET permission
    if grep -q "android.permission.INTERNET" "$manifest"; then
        echo "❌ ERROR: Found android.permission.INTERNET in $manifest"
        echo "   This violates the PRD privacy requirements!"
        echo "   Crispify must NOT have network permissions."
        exit 1
    fi
    
    # Check for ACTION_PROCESS_TEXT intent filter
    if grep -q "android.intent.action.PROCESS_TEXT" "$manifest"; then
        echo "✅ ACTION_PROCESS_TEXT intent filter found"
    else
        echo "⚠️  WARNING: No ACTION_PROCESS_TEXT intent filter found"
        echo "   This is required for the v1.0 text selection feature"
    fi
done

echo "✅ Manifest validation passed - No network permissions found"