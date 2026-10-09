---
name: create-issue
description: Create a well-specified GitHub issue for this Spring Boot assignment. Accepts "Q1".."Q5" to build the issue from the assignment spec, or a free-text description for other work. Use when the user wants to log a question, feature, bug, or task as an issue.
argument-hint: <Q1..Q5 | short description>
allowed-tools: Bash(gh issue:*), Bash(gh label:*), Bash(gh repo view:*), Read, Grep, Glob, Write
---

# Create issue

Input: `$ARGUMENTS`.

## 1. Source the requirements
- If the input is `Q<n>` (case-insensitive, `n` in 1–5): read `docs/ASSIGNMENT.md` and copy that question's Requirements and Acceptance criteria **verbatim** — they are the contract the interviewer checks.
- Otherwise treat the input as a free-text description. Ask the user only if the expected behaviour is genuinely ambiguous.
- Before creating, check for duplicates: `gh issue list --state all --search "Q<n> in:title"`. If one exists, report it instead of creating another.

## 2. Draft
- Title: `Q<n> — <question name>` for assignment questions (e.g. `Q1 — Task Manager API`), otherwise an imperative summary (≤ 70 chars).
- Body (write to a scratchpad file):

```markdown
## Context
One or two lines on what this delivers.

## Requirements
- (verbatim from the spec)

## Acceptance Criteria
- [ ] (verbatim from the spec)
- [ ] Automated tests cover the behaviour and error paths; `./mvnw verify` passes

## Technical Notes
Planned design: packages/classes, endpoints (method + path + status codes), key decisions to be explained in the PR.

## Branch
`feature/q<n>-<slug>` (exact name from CLAUDE.md)
```

## 3. Label
Ensure the label exists (idempotent): `gh label create question --force --color 1D76DB --description "Assignment question"` for Q-issues; otherwise `feature`/`bug`/`chore` as appropriate.

## 4. Create
`gh issue create --title "<title>" --body-file <file> --label <label>`

## 5. Report
Issue number + URL, and the next step: `/start-issue <number>`.
