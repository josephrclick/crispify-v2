# Agent OS Workflow Instructions

## CRITICAL: Understanding Workflow Phases

When executing `/execute-tasks`, you MUST complete ALL THREE phases:

1. **Phase 1**: Setup (Steps 1-3)
   - Task assignment
   - Context gathering
   - **Git branch creation** (MANDATORY - never skip)

2. **Phase 2**: Task execution (Step 4)
   - Loop through all assigned tasks
   - Mark tasks complete

3. **Phase 3**: Post-execution (Step 5)
   - **MANDATORY**: Do NOT stop after Phase 2
   - **AUTOMATIC**: Proceed without asking permission
   - Load and execute post-execution-tasks.md

## Branch Management Rules

1. **NEVER** work directly on main branch
2. **ALWAYS** create feature branch in Step 3
3. Branch naming: Remove date prefix from spec folder name
   - Spec folder: `2025-08-28-first-launch-screen`
   - Branch name: `first-launch-screen`

## Subagent Usage

When instructions contain `subagent="X"`:
- Use the Task tool
- Set `subagent_type` to "X"
- Never skip subagent steps
- See `.agent-os/docs/subagent-usage-guide.md` for examples

## Post-Execution Requirements

After all tasks complete, you MUST:
1. Run full test suite
2. Create git commit
3. Push to GitHub
4. Create pull request
5. Update tasks.md with [x] markers
6. Create recap document
7. Play completion sound

## Common Mistakes to Avoid

❌ Stopping after task execution to ask "Should I continue?"
❌ Working on main branch
❌ Skipping subagent steps
❌ Not marking tasks complete in tasks.md
❌ Forgetting post-execution workflow


