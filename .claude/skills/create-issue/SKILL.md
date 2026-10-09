---
name: create-issue
description: Create a well-specified GitHub issue (context, requirements, acceptance criteria, technical notes) for this Spring Boot project. Use when the user wants to log a feature, bug, or task as an issue.
argument-hint: <short description of the feature / bug / task>
allowed-tools: Bash(gh issue:*), Bash(gh label:*), Bash(gh repo view:*), Read, Grep, Glob, Write
---

# Create issue

Input: `$ARGUMENTS` — a free-text description of the work.

## Steps

1. **Understand the request.** Read `CLAUDE.md` and skim relevant code (Grep/Glob) so the issue references real packages, entities and endpoints. Ask the user a question only if the scope is genuinely ambiguous (e.g. unclear expected behaviour); otherwise make sensible assumptions and list them under Technical Notes.

2. **Pick a type**: `feat`, `fix`, `chore`, `refactor`, `test` or `docs`. Map to a label: feat→`feature`, fix→`bug`, others→same name.

3. **Draft the issue.**
   - Title: `<type>: <imperative summary>` (≤ 70 chars), e.g. `feat: add endpoint to create an order`.
   - Body (write to a temp file in the scratchpad):

   ```markdown
   ## Context
   Why this is needed / the problem.

   ## Requirements
   - Concrete behaviour, one bullet each.

   ## Acceptance Criteria
   - [ ] Testable, observable outcome (HTTP method + path + status codes, validation rules, persisted state)
   - [ ] Error cases (400 on invalid input, 404 when missing, ...)
   - [ ] Unit + slice tests cover the above and `./mvnw verify` passes

   ## Technical Notes
   Affected packages/classes, API contract (request/response JSON), entities, assumptions.

   ## Out of Scope
   What this issue deliberately does not cover.
   ```

4. **Ensure the label exists** (idempotent):
   `gh label create <label> --force --color <hex>` — feature `1D76DB`, bug `D73A4A`, chore `C5DEF5`, refactor `FBCA04`, test `0E8A16`, docs `0075CA`.

5. **Show the draft** title + body to the user, then create it:
   `gh issue create --title "<title>" --body-file <file> --label <label>`

6. **Report** the issue number and URL, and suggest `/start-issue <number>` as the next step.
