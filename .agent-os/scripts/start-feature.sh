#!/bin/bash
# Usage: ./start-feature.sh <spec-folder-name>

set -euo pipefail

SPEC_FOLDER=${1:-}

if [ -z "$SPEC_FOLDER" ]; then
    echo "Usage: $0 <spec-folder-name>"
    echo "Example: $0 2025-08-28-first-launch-screen"
    exit 1
fi

# Extract feature name (remove date prefix)
FEATURE_NAME=$(echo "$SPEC_FOLDER" | sed 's/^[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}-//')

echo "Creating feature branch: $FEATURE_NAME"

# Ensure we're on main and up to date
git checkout main
git pull origin main

# Create and switch to feature branch
git checkout -b "$FEATURE_NAME"

echo "✅ Switched to branch: $FEATURE_NAME"
echo "📁 Working on spec: .agent-os/specs/$SPEC_FOLDER/"


