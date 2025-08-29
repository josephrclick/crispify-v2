#!/bin/bash
# Helper script for agents to commit completed parent tasks
# Usage: ./commit-task.sh <task-number> <task-description> <spec-folder>

set -e

TASK_NUMBER="$1"
TASK_DESCRIPTION="$2"
SPEC_FOLDER="$3"

# Validate inputs
if [ -z "$TASK_NUMBER" ] || [ -z "$TASK_DESCRIPTION" ] || [ -z "$SPEC_FOLDER" ]; then
    echo "❌ Usage: $0 <task-number> <task-description> <spec-folder>"
    echo "   Example: $0 3 'Implement validation logic' '2025-08-29-process-text-intent-handler'"
    exit 1
fi

# Check if we're on a feature branch (not main)
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ]; then
    echo "❌ ERROR: Cannot commit directly to main branch!"
    echo "   Please switch to a feature branch first."
    exit 1
fi

echo "📝 Committing parent task $TASK_NUMBER: $TASK_DESCRIPTION"

# Stage all changes
git add -A

# Check if there are changes to commit
if git diff --cached --quiet; then
    echo "⚠️  No changes to commit for task $TASK_NUMBER"
    exit 0
fi

# Create commit message
COMMIT_MSG="feat: $TASK_DESCRIPTION

- Completed parent task $TASK_NUMBER
- Updated tasks.md with completion status
- All tests passing for this task

Task: $TASK_NUMBER from spec $SPEC_FOLDER"

# Create the commit
git commit -m "$COMMIT_MSG"

echo "✅ Task $TASK_NUMBER committed successfully"
echo "   Branch: $CURRENT_BRANCH"
echo "   Commit: $(git rev-parse --short HEAD)"