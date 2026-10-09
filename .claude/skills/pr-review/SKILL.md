---
name: pr-review
description: Review a GitHub pull request against its linked issue / assignment acceptance criteria, the project conventions and the interviewer's follow-up questions; run the build and post the findings as a GitHub review with inline comments. Use when the user asks to review a PR.
argument-hint: "[pr number]"
---

# PR review

Input: `$ARGUMENTS` — PR number; else the current branch's PR (`gh pr view --json number`); else ask.

## 1. Gather context
- `gh pr view <P> --json number,title,body,author,headRefName,headRefOid,baseRefName,files,url`
- `gh pr diff <P>`
- Linked issue from `Closes #N`: `gh issue view <N> --json title,body`. For `Q<n>` PRs also read that question in `docs/ASSIGNMENT.md`.
- `gh api user --jq .login` (reviewer login), and `CLAUDE.md`.

## 2. Build
If the working tree is clean: remember the current branch, `gh pr checkout <P>`, run `./mvnw -q verify`, record pass/fail, then switch back. Otherwise skip and say so.

## 3. Review
Read changed files in full where context matters. Check:
1. **Acceptance criteria** — each met, with evidence (file:line or test name).
2. **Tests** — every criterion and error path tested; tests assert real outcomes; concurrency tests actually run in parallel.
3. **Correctness** — edge cases, null handling, transactions, races (what breaks if two requests hit this at once?).
4. **API** — HTTP methods/status codes, DTOs not entities, errors via shared `ApiError`.
5. **Security** — injection, hard-coded secrets, missing authz, sensitive data leaks.
6. **Persistence** — N+1, missing constraints/indexes, lost updates.
7. **PR discipline** — template sections filled meaningfully, commit messages meaningful (no `fix`/`final`), README row updated, branch name matches CLAUDE.md, scope limited to the question.
8. **Explainability** — flag code that is non-obvious enough that the author should be ready to explain it in the interview.

Verify each finding against the code; report only real issues. Severity: **blocker**, **major**, **minor**, **nit**.

## 4. Post
Payload in a scratchpad JSON file:
`gh api repos/{owner}/{repo}/pulls/<P>/reviews --method POST --input <file>`
`{"commit_id": "<headRefOid>", "event": "<EVENT>", "body": "<summary>", "comments": [{"path": "...", "line": N, "side": "RIGHT", "body": "**[major]** ..."}]}`

Summary body:
```markdown
## Review summary
**Build:** ✅ / ❌ `./mvnw verify`
### Acceptance criteria
- ✅/❌ criterion — evidence
### Findings
blocker: X · major: X · minor: X · nit: X
### Interview prep
Questions the interviewer is likely to ask about this PR, with short answers.
**Verdict:** Approve / Changes requested
```

EVENT: blockers/majors → `REQUEST_CHANGES`, else `APPROVE`. If reviewer login == PR author (self-review), GitHub rejects both — use `COMMENT` and state the verdict in the body. If an inline comment is rejected (line not in diff), move it to the summary and retry.

## 5. Report
Print the summary and review URL. Never merge.
