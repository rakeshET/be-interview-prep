---
name: start-issue
description: Start work on a GitHub issue - create a branch from latest main, assign the issue, plan against its acceptance criteria, implement with tests until ./mvnw verify is green, and commit. Use when the user says to start/pick up/work on an issue.
argument-hint: <issue number>
---

# Start issue

Input: `$ARGUMENTS` — the issue number (strip a leading `#`). If missing, run `gh issue list --assignee @me --state open` and ask which one.

## 1. Load the issue
`gh issue view <N> --json number,title,body,labels,state,url`
- If `state` is `CLOSED`, stop and tell the user.
- Extract the Requirements and Acceptance Criteria — they are the definition of done.

## 2. Prepare the branch
1. `git status --porcelain` must be empty. If not, stop and ask the user what to do with the changes.
2. `git fetch origin`.
3. If `origin/main` does not exist (empty repo), tell the user `main` needs an initial commit first and stop.
4. Derive the branch name `<type>/<N>-<slug>`:
   - type from the title prefix (`feat:` → `feat`, etc.) or label (feature→feat, bug→fix); default `feat`.
   - slug: title without prefix, lowercase, non-alphanumerics → `-`, max ~5 words.
5. If the branch already exists locally or on origin, switch to it and continue. Otherwise: `git switch -c <branch> origin/main`.
6. `gh issue edit <N> --add-assignee @me`.

## 3. Plan
Read `CLAUDE.md` and the relevant code. Produce a short plan mapping **each acceptance criterion → the classes/tests that satisfy it**. If the Spring Boot skeleton is missing, bootstrapping it (per `CLAUDE.md`) is the first step. Share the plan with the user before writing code.

## 4. Implement
- Follow the conventions in `CLAUDE.md` (layering, DTO records, validation, `@RestControllerAdvice`, constructor injection).
- Write/extend tests alongside the code: service unit tests, `@WebMvcTest` for controllers (happy path + error paths), `@DataJpaTest` for custom queries.
- Run `./mvnw -q verify` (PowerShell: `.\mvnw.cmd -q verify`) and fix failures until green. Do not skip or disable tests to get green.
- Stay in scope: anything outside the issue goes into a note for a follow-up `/create-issue`, not this branch.

## 5. Commit
- Small logical Conventional Commits referencing the issue, e.g. `feat(order): add create order endpoint (#<N>)`.
- End each commit message with the co-author attribution line required by the session.
- Do not push and do not open a PR — that is `/raise-pr`.

## 6. Report
Summarise what was done per acceptance criterion, the test results, any follow-ups, and suggest `/raise-pr`.
