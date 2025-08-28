# Feature Branch Workflow Implementation Plan

## Overview
This document outlines a comprehensive workflow to ensure all development work happens on feature branches, with automated reviews and controls to protect the main branch.

## Workflow Components

### 1. Agent Instructions File (`.agent-os/agent-instructions.md`)
Create a dedicated instructions file that all agents must read first containing:
- Clear workflow requirements
- Branch naming conventions (e.g., `feature/issue-{number}-{description}`)
- Mandatory steps before starting work
- PR creation guidelines

### 2. Pre-work Git Hook (`pre-commit` hook)
Install a git hook that:
- Prevents direct commits to main branch
- Shows warning message directing to create feature branch
- Can be bypassed with `--no-verify` for emergency fixes

Example hook implementation:
```bash
#!/bin/sh
branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" = "main" ]; then
  echo "Direct commits to main branch are not allowed."
  echo "Please create a feature branch: git checkout -b feature/issue-XX-description"
  echo "To bypass (emergency only): git commit --no-verify"
  exit 1
fi
```

### 3. GitHub Branch Protection Rules
Configure via GitHub repository settings:
- Require PR reviews before merging to main
- Require status checks (Claude Code Review) to pass
- Dismiss stale PR approvals when new commits are pushed
- Optionally: Require branches to be up-to-date before merging

### 4. Workflow Helper Script (`.agent-os/scripts/start-feature.sh`)
Create a helper script for starting new features:
```bash
#!/bin/bash
# Usage: ./start-feature.sh <issue-number> <description>

ISSUE_NUM=$1
DESCRIPTION=$2
BRANCH_NAME="feature/issue-${ISSUE_NUM}-${DESCRIPTION}"

git checkout main
git pull origin main
git checkout -b "$BRANCH_NAME"
git push -u origin "$BRANCH_NAME"

echo "Created and pushed branch: $BRANCH_NAME"
echo "Next steps:"
echo "1. Make your changes"
echo "2. Commit with: git commit -m 'feat: description (#${ISSUE_NUM})'"
echo "3. Create PR with: gh pr create --title 'Feature: ${DESCRIPTION} (#${ISSUE_NUM})'"
```

### 5. Update CLAUDE.md
Add explicit git workflow instructions:
- Reference to agent instructions
- Command to run before starting work
- PR creation requirements
- Link to workflow documentation

Example addition:
```markdown
## Git Workflow

**IMPORTANT**: All development must happen on feature branches. Never commit directly to main.

Before starting work:
1. Read `.agent-os/agent-instructions.md` for workflow requirements
2. Create a feature branch: `./agent-os/scripts/start-feature.sh <issue-number> <description>`
3. Make all commits to the feature branch
4. Create PR when work is complete using `gh pr create`

See `.agent-os/docs/github-workflow.md` for complete workflow documentation.
```

### 6. GitHub Issue Templates
Create `.github/ISSUE_TEMPLATE/feature-request.md`:
```markdown
---
name: Feature Request
about: Suggest a new feature for development
title: '[FEATURE] '
labels: enhancement
assignees: ''
---

## Description
Brief description of the feature

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2

## Technical Notes
Any technical considerations

## Suggested Branch Name
`feature/issue-XX-description`

## PR Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] Claude Code Review passes
```

### 7. Manual Trigger Points

#### Developer Responsibilities:
1. **Create GitHub issues** with specifications
2. **Assign issues** to agents or specify which issue to work on
3. **Review and merge PRs** after Claude Code Review passes

#### Agent Responsibilities:
1. **Read issue** and understand requirements
2. **Create feature branch** using helper script
3. **Implement solution** on feature branch
4. **Create PR** when complete
5. **Address review feedback** if needed

## Implementation Benefits

- **Simple**: Mostly configuration and documentation
- **Flexible**: Maintainer retains control over key decisions
- **Safe**: Multiple safeguards prevent accidental main branch changes
- **Clear**: Agents have explicit instructions to follow
- **Automated where useful**: GitHub Actions handle PR reviews

## Implementation Checklist

1. [ ] Create `.agent-os/agent-instructions.md` file
2. [ ] Update `CLAUDE.md` with workflow instructions
3. [ ] Install pre-commit hook in `.git/hooks/pre-commit`
4. [ ] Create helper script at `.agent-os/scripts/start-feature.sh`
5. [ ] Configure GitHub branch protection rules
6. [ ] Create GitHub issue templates
7. [ ] Test workflow with a sample feature

## Example Workflow

1. **Human**: Creates issue #1 "Implement ProcessTextActivity"
2. **Human**: Tells agent "Work on issue #1"
3. **Agent**: Runs `./start-feature.sh 1 process-text-activity`
4. **Agent**: Implements feature on `feature/issue-1-process-text-activity`
5. **Agent**: Creates PR with `gh pr create`
6. **GitHub Actions**: Runs Claude Code Review
7. **Human**: Reviews and merges PR if checks pass

## Notes

- The `--no-verify` flag should only be used for emergency hotfixes
- Consider implementing semantic commit messages (feat:, fix:, docs:, etc.)
- Branch protection rules require repository admin access to configure
- GitHub CLI (`gh`) must be authenticated for PR creation to work