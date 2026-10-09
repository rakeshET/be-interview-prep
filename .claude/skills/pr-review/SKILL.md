---
name: pr-review
description: Review a GitHub pull request against its linked issue's acceptance criteria and the project's Spring Boot conventions, run the build, and post the findings as a GitHub review with inline comments. Use when the user asks to review a PR.
argument-hint: "[pr number]"
---

# PR review

Input: `$ARGUMENTS` — PR number. If absent, use the PR for the current branch (`gh pr view --json number`). If none, ask.

## 1. Gather context
- `gh pr view <P> --json number,title,body,author,headRefName,headRefOid,baseRefName,files,url`
- `gh pr diff <P>`
- Linked issue: find `Closes #N` in the body, then `gh issue view <N> --json title,body` → acceptance criteria.
- `gh api user --jq .login` → reviewer login (needed for step 5).
- Read `CLAUDE.md` for the conventions to review against.

## 2. Build
1. Note the current branch, ensure the working tree is clean (if not, skip local build and say so).
2. `gh pr checkout <P>` and run `./mvnw -q verify` (PowerShell: `.\mvnw.cmd -q verify`). Record pass/fail and failing tests.
3. Switch back to the original branch afterwards.

## 3. Review
Read every changed file in full (not just the hunks) where needed for context. Check:
1. **Acceptance criteria** — each one met? Evidence (file:line or test name).
2. **Correctness** — logic errors, null handling, edge cases, transactions (`@Transactional` placement), off-by-one, concurrency.
3. **API design** — correct HTTP methods/status codes, DTOs not entities, consistent `ProblemDetail` errors.
4. **Validation & error handling** — Bean Validation on inputs, exceptions mapped in `@RestControllerAdvice`, no swallowed exceptions.
5. **Security** — injection (string-built JPQL/SQL), secrets in code/config, missing authz, sensitive data in logs or responses.
6. **Persistence** — N+1 queries, missing indexes/constraints, lazy-loading leaks.
7. **Tests** — new behaviour and error paths covered; tests assert meaningful outcomes; no disabled tests.
8. **Conventions & readability** — layering, constructor injection, naming, dead code, scope creep beyond the issue.

Only report real issues; verify each finding against the code before including it. Classify as **blocker** (wrong behaviour, failing build, security hole, unmet acceptance criterion), **major**, **minor**, or **nit**.

## 4. Compose the review
- Inline comments: one per finding, anchored to a line that appears in the diff on the RIGHT side, body prefixed with severity, e.g. `**[major]** ... ` plus a suggested fix (use a ```suggestion block for small fixes).
- Findings that can't be anchored to a diff line go in the summary.
- Summary body:

```markdown
## Review summary
**Build:** ✅ `./mvnw verify` passed / ❌ failed (details)

### Acceptance criteria
- ✅/❌ criterion — evidence

### Findings
blocker: X · major: X · minor: X · nit: X

**Verdict:** Approve / Changes requested / Comments only
```

## 5. Post
Write the payload to a scratchpad JSON file and send:
`gh api repos/{owner}/{repo}/pulls/<P>/reviews --method POST --input <file>`
with `{"commit_id": "<headRefOid>", "event": "<EVENT>", "body": "<summary>", "comments": [{"path": "...", "line": N, "side": "RIGHT", "body": "..."}]}`.

EVENT:
- blockers or majors → `REQUEST_CHANGES`
- none → `APPROVE`
- If the reviewer login equals the PR author, GitHub rejects APPROVE/REQUEST_CHANGES — use `COMMENT` and state the intended verdict in the summary.

If the API rejects an inline comment (line not in diff), move that finding into the summary and retry.

## 6. Report
Print the summary and the review URL in the terminal.
