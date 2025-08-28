#!/bin/bash

# Check for network-related dependencies in build.gradle files
# Per PRD: App must not have network capabilities

echo "🔍 Checking dependencies for network-related libraries..."

GRADLE_FILES=$(find . -name "*.gradle" -o -name "*.gradle.kts" | grep -v build/)

NETWORK_KEYWORDS=(
    "retrofit"
    "okhttp"
    "volley"
    "ktor"
    "apache.http"
    "java.net"
    "javax.net"
    "socket"
    "websocket"
    "grpc"
    "apollo"
    "firebase"
    "analytics"
    "crashlytics"
    "networking"
    "http-client"
    "rest-client"
)

FOUND_ISSUES=0

for gradle_file in $GRADLE_FILES; do
    for keyword in "${NETWORK_KEYWORDS[@]}"; do
        if grep -qi "$keyword" "$gradle_file"; then
            echo "⚠️  Warning: Found potential network library '$keyword' in $gradle_file"
            FOUND_ISSUES=1
        fi
    done
done

# Check for Play Asset Delivery configuration
if grep -q "play-asset-delivery" build.gradle* 2>/dev/null || grep -q "com.android.asset-pack" build.gradle* 2>/dev/null; then
    echo "✅ Play Asset Delivery configuration found (required for model delivery)"
else
    echo "⚠️  Warning: No Play Asset Delivery configuration found"
    echo "   This is required for GGUF model delivery per PRD"
fi

if [ $FOUND_ISSUES -eq 0 ]; then
    echo "✅ No network-related dependencies found"
else
    echo "⚠️  Please review network-related dependencies above"
    echo "   Remember: Crispify must maintain absolute privacy with no network access"
fi