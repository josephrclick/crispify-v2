#!/bin/bash
# Install git hooks for Agent OS

set -euo pipefail

echo "Installing git hooks..."

# Install pre-commit hook
if [ -f .claude/hooks/pre-commit-checks.sh ]; then
    ln -sf ../../.claude/hooks/pre-commit-checks.sh .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    echo "✅ Pre-commit hook installed"
else
    echo "❌ Pre-commit hook script not found"
fi

echo "Git hooks installation complete"


