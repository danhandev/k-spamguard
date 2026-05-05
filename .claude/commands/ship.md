Commit all current work, push, and create or update a pull request.

Steps to follow:
1. Run `git status` and `git diff` to understand what changed.
2. Run `git log --oneline -5` to see recent commits for context.
3. Stage relevant files (never stage .env, *.key, *.pem, secrets):
   - Stage modified tracked files: `git add --update`
   - Stage new safe files by extension: *.java, *.kt, *.gradle, *.yml, *.yaml, *.json, *.md, *.sql, *.ts, *.tsx, *.css, Dockerfile, .gitignore
4. Write a conventional commit message:
   - Format: `<type>(<scope>): <subject>` — e.g. `feat(webhook): add idempotency key deduplication`
   - Types: feat, fix, refactor, test, docs, chore, security
   - Body: bullet list of what changed and why
5. Commit: `git commit -m "..."`
6. Push: `git push -u origin <current-branch>`
7. Check if a PR already exists for this branch: `gh pr view --json number,url`
   - If no PR: create one with `gh pr create --title "..." --body "..." `
     - Auto-detect issue number from branch name (e.g. `feat/issue-42-foo` → Closes #42)
     - Use the PR template structure: Summary, Changes, Acceptance Criteria, Security Checklist
     - Create as draft if work is incomplete, ready for review if complete
   - If PR exists: print the existing PR URL
8. Report: commit hash, PR URL, and any remaining tasks.

Do not force-push. Do not commit on main.
