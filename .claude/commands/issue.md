Create a GitHub issue for the task, then set up a feature branch.

If the user provided a task description in the command arguments, use it. Otherwise ask for a one-line title and brief description.

Steps to follow:
1. Determine the issue type: `feat` (new feature), `fix` (bug), `chore` (infra/docs/refactor).
2. Create the GitHub issue:
   ```
   gh issue create --title "<title>" --body "<description>\n\n## Acceptance Criteria\n- [ ] " --label <type>
   ```
3. Capture the issue number from the output URL (e.g. #42).
4. Create a slug from the title (lowercase, spaces → hyphens, max 40 chars).
5. Create and checkout the branch:
   ```
   git checkout -b <type>/issue-<number>-<slug>
   ```
6. Push the branch to origin:
   ```
   git push -u origin <branch>
   ```
7. Report back:
   - Issue URL
   - Branch name
   - What to implement next

Always confirm the issue title and branch name with the user before creating.
