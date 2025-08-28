# Subagent Usage Guide for Agent OS

## What are Subagents?

Subagents are specialized agents invoked through the Task tool to handle specific workflows.

## How to Use Subagents

When you see instructions like:

```xml
<step number="3" subagent="git-workflow" name="git_branch_management">
```

You MUST:
1. Use the Task tool
2. Set `subagent_type` parameter to the subagent value (e.g., "git-workflow")
3. Provide the prompt as specified in the instructions

## Example Translation

Instruction:
```
Use the git-workflow subagent to manage branches for spec: [SPEC_FOLDER]
```

Implementation:
```
Use Task tool with:
- subagent_type: "git-workflow"
- prompt: "Check and manage branch for spec: [SPEC_FOLDER]..."
- description: "Git branch management"
```

## Available Subagents

| Subagent | Purpose | Location |
|----------|---------|----------|
| git-workflow | Branch management, commits, PRs | .claude/agents/git-workflow.md |
| context-fetcher | Retrieve relevant docs | .claude/agents/context-fetcher.md |
| test-runner | Run and analyze tests | .claude/agents/test-runner.md |
| project-manager | Track tasks, create recaps | .claude/agents/project-manager.md |
| file-creator | Create files and directories | .claude/agents/file-creator.md |
| date-checker | Get current date | .claude/agents/date-checker.md |


