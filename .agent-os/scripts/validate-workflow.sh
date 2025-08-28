#!/bin/bash
# Validate Agent OS workflow setup

set -euo pipefail

echo "🔍 Validating Agent OS Workflow Setup..."

# Check git hooks
if [ -L .git/hooks/pre-commit ]; then
    echo "✅ Pre-commit hook installed"
else
    echo "❌ Pre-commit hook missing - run .agent-os/scripts/install-hooks.sh"
fi

# Check current branch
BRANCH=$(git branch --show-current)
if [ "$BRANCH" = "main" ]; then
    echo "⚠️  WARNING: Currently on main branch"
else
    echo "✅ On feature branch: $BRANCH"
fi

# Check for uncommitted changes
if git diff-index --quiet HEAD --; then
    echo "✅ No uncommitted changes"
else
    echo "⚠️  Uncommitted changes detected"
fi

# Check for agent documentation
if [ -f .agent-os/agent-instructions.md ]; then
    echo "✅ Agent instructions present"
else
    echo "❌ Agent instructions missing"
fi

if [ -f .agent-os/docs/subagent-usage-guide.md ]; then
    echo "✅ Subagent guide present"
else
    echo "❌ Subagent guide missing"
fi

echo "Validation complete"


