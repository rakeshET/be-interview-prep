---
name: raise-pr
description: Verify the build, push the current issue branch, and open (or update) a GitHub pull request linked to its issue with a summary, test notes and acceptance-criteria checklist. Use when the user wants to raise/open/create a PR.
argument-hint: "[issue number]"
---

# Raise PR

Input: `$ARGUMENTS` — optional issue number. If absent, parse it from the branch name `<type>/<N>-<slug>`. If neither works, ask.

## 1. Preconditions
1. `git rev-parse --abbrev-ref HEAD` — if `main`, stop: PRs must come from an issue branch (suggest `/start-issue`).
2. `git status --porcelain` — if there are uncommitted changes, show them and ask whether to commit them (Conventional Commit referencing `#N`) or stop.
3. `git fetch origin` and `git log --oneline origin/main..HEAD` — if there are no commits, stop.
4. If `origin/main` has moved ahead, rebase is the user's call: tell them and ask before running `git rebase origin/main`.

## 2. Verify
Run `./mvnw -q verify` (PowerShell: `.\mvnw.cmd -q verify`). If it fails, stop and report the failures — never open a PR on a red build.

## 3. Push
`git push -u origin HEAD` (never force-push; if rejected, report and ask).

## 4. Build the PR
- `gh issue view <N> --json title,body` for the acceptance criteria.
- `git log --format='- %s' origin/main..HEAD` and `git diff --stat origin/main...HEAD` for the change list.
- Title: `<type>(<scope>): <summary> (#<N>)` — derive from the issue title.
- Body (write to a scratchpad file), following `.github/pull_request_template.md`:

```markdown
## Summary
What changed and why, 2–4 sentences.

## Changes
- Bullet per meaningful change (endpoints, entities, config, tests)

## Acceptance Criteria
- [x] criterion copied from the issue (tick only those actually satisfied)
- [ ] criterion not done — with a reason

## How to Test
`./mvnw verify`, plus sample `curl` requests for new endpoints.

## Notes
Assumptions, follow-ups, anything reviewers should focus on.

Closes #<N>

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## 5. Create or update
- `gh pr view --json number,url` on the current branch. If a PR exists: `gh pr edit <num> --title ... --body-file ...`.
- Otherwise: `gh pr create --base main --head <branch> --title "<title>" --body-file <file>` and add the issue's label with `--label`.

## 6. Report
Print the PR URL and suggest `/pr-review <number>`.
