---
name: raise-pr
description: Verify the build, update the README PR-link row, push the current feature branch, and open (or update) a GitHub pull request using the assignment's PR template (Problem / Approach / Decisions & trade-offs / How to test). Never merges. Use when the user wants to raise/open/create a PR.
argument-hint: "[issue number]"
---

# Raise PR

Input: `$ARGUMENTS` — optional issue number. Otherwise find it with `gh issue list --search "Q<n> in:title"` using the `q<n>` in the branch name. If neither works, ask.

## 1. Preconditions
1. Current branch must not be `main` (else stop, suggest `/start-issue`).
2. `git status --porcelain`: if dirty, show the changes and ask whether to commit them.
3. `git fetch origin`; `git log --oneline origin/main..HEAD` must be non-empty.
4. If `origin/main` has moved ahead of the branch base, tell the user and ask before rebasing.

## 2. Verify
`./mvnw -q verify` (PowerShell `.\mvnw.cmd -q verify`). On failure stop and report — never open a PR on a red build. Confirm the branch adds tests (`git diff --stat origin/main...HEAD -- src/test`); if none, stop: every PR must include tests.

## 3. Push and open the PR
1. `git push -u origin HEAD` (never force).
2. Title: `Q<n> — <question name>` (e.g. `Q1 — Task Manager API`).
3. Body (scratchpad file) — the assignment's template, filled with real content:

```markdown
## Problem
What this PR solves (1–2 lines).

## Approach
Key classes and how a request flows through them (controller → service → repository → DB).

## Decisions & trade-offs
Each key choice, the alternatives considered, and why — especially concurrency, idempotency, caching, security.

## How to test
`./mvnw verify`, the test classes and what each proves, and sample `curl` requests.

Closes #<N>

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

4. If a PR exists for the branch (`gh pr view --json number`), `gh pr edit`; else `gh pr create --base main --title ... --body-file ... --label question`.

## 4. Update the README row
Edit the question's row in the README PR-link table to `[#<pr>](<pr url>)`. Commit `Add Q<n> PR link to README` and push to the same branch.

## 5. Stop
Print the PR URL and say: **ready for you to review and merge; after merging, tell me to continue with the next question.** Claude never runs `gh pr merge`. Suggest `/pr-review <number>` for a self-review.
