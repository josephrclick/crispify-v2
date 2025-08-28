---
description: Common Pre-Flight Steps for Agent OS Instructions
globs:
alwaysApply: false
version: 1.0
encoding: UTF-8
---

# Pre-Flight Rules

- IMPORTANT: For any step that specifies a subagent in the subagent="" XML attribute you MUST use the specified subagent to perform the instructions for that step.

- HOW: Use the Task tool with:
  - subagent_type: the value from the subagent attribute (e.g., "git-workflow")
  - prompt: copy the REQUEST text verbatim
  - description: short purpose of the step
  See `.agent-os/docs/subagent-usage-guide.md` for examples.

- Process XML blocks sequentially

- Read and execute every numbered step in the process_flow EXACTLY as the instructions specify.

- If you need clarification on any details of your current task, stop and ask the user specific numbered questions and then continue once you have all of the information you need.

- Use exact templates as provided
