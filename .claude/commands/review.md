Perform a code review of the changed files in the current branch.

@.claude/agents/code-reviewer.md

First, identify the changed files:
```
git diff --name-only main...HEAD
```

For each changed Java file, apply the full review checklist from the code-reviewer persona above.

Output format for each issue:
- **File**: `path/to/File.java:line`
- **Category**: Bug | Standards Violation | Missing Test | Code Smell | Suggestion
- **Rule**: which rule or checklist item is violated
- **Fix**: concrete suggestion

At the end, provide a summary: total issues by category, overall assessment (Approve / Request Changes).
