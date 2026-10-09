---
name: start-issue
description: Start work on a GitHub issue - create the assignment branch from latest main, assign the issue, plan against its acceptance criteria, implement with tests until ./mvnw verify is green, and commit in small steps. Use when the user says to start/pick up/work on an issue or question.
argument-hint: <issue number>
---

# Start issue

Input: `$ARGUMENTS` — issue number (strip a leading `#`). If missing, `gh issue list --state open` and ask which one.

## 1. Load the issue
`gh issue view <N> --json number,title,body,labels,state,url`
- If `CLOSED`, stop.
- Extract Requirements and Acceptance Criteria — the definition of done.
- If the title starts with `Q<n>`, also re-read that question in `docs/ASSIGNMENT.md`.

## 2. Gate on the previous question
For `Q<n>` with n > 1: the PR for `Q<n-1>` must be merged.
`gh pr list --state merged --search "Q<n-1> in:title"` — if empty, stop and tell the user to merge the previous PR first. Claude never merges.

## 3. Prepare the branch
1. `git status --porcelain` must be empty; otherwise stop and ask.
2. `git switch main && git pull --ff-only origin main` (start from the latest main, as the assignment requires).
3. Branch name — use the exact mapping from CLAUDE.md:
   Q1 `feature/q1-task-api`, Q2 `feature/q2-url-shortener`, Q3 `feature/q3-auth`, Q4 `feature/q4-product-catalog`, Q5 `feature/q5-order-service`.
   For non-question issues: `feature/<N>-<slug>`.
4. If the branch exists, switch to it; otherwise `git switch -c <branch>`.
5. `gh issue edit <N> --add-assignee @me`.

## 4. Plan
Read `CLAUDE.md` and the relevant code. Map **each acceptance criterion → classes + the test that proves it**. Decide the design choices the interviewer will ask about (concurrency, alternatives) and note them for the PR's "Decisions & trade-offs".

## 5. Implement
- Follow CLAUDE.md conventions (feature packages, DTO records, Bean Validation, shared `ApiError` via `GlobalExceptionHandler`, constructor injection, no secrets in code).
- **Tests are mandatory**: every acceptance criterion and error path gets a test (MockMvc for HTTP contracts, `@SpringBootTest` for flows/concurrency, unit tests for services).
- Run `./mvnw -q verify` (PowerShell `.\mvnw.cmd -q verify`) until green. Never skip or disable tests. Keep earlier questions' tests green.
- Stay in scope; anything else becomes a note for a follow-up issue.

## 6. Commit
Small, meaningful commits with plain imperative messages, e.g. `Add create task endpoint`, `Return field errors for invalid input`, `Add tests for task validation`. Never `fix`/`changes`/`final`. End each message with the session's co-author attribution line. Don't push or open a PR — that's `/raise-pr`.

## 7. Report
Per acceptance criterion: done + which test proves it. Then suggest `/raise-pr`.
