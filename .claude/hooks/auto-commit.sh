#!/usr/bin/env bash
# Runs on Claude Code Stop event: auto-stages, commits, pushes, and drafts PR.
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
cd "$ROOT"

BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null) || exit 0

# Never auto-commit on main
[[ "$BRANCH" == "main" || "$BRANCH" == "master" ]] && exit 0

# Nothing changed at all?
git diff --quiet && git diff --staged --quiet && exit 0

# Stage safe file types — never touch .env or secrets
git add -- \
  '*.java' '*.kt' '*.gradle' '*.kts' '*.groovy' \
  '*.yml' '*.yaml' '*.xml' '*.properties' \
  '*.json' '*.md' '*.sql' '*.sh' \
  '*.ts' '*.tsx' '*.js' '*.jsx' \
  '*.css' '*.scss' '*.html' \
  'Dockerfile' '.gitignore' \
  2>/dev/null || true

# Also re-stage tracked files that are modified
git add --update 2>/dev/null || true

# Unstage anything that looks sensitive
git restore --staged -- \
  '**/.env' '**/.env.*' '**/*.key' '**/*.pem' \
  '**/*.p12' '**/*.jks' '**/secrets' \
  2>/dev/null || true

# Nothing staged after filtering?
git diff --staged --quiet && exit 0

# Build commit message from staged file list
CHANGED=$(git diff --staged --name-only | head -8 | paste -sd ', ')
TIMESTAMP=$(date '+%Y-%m-%d %H:%M')

git commit -m "auto: claude session [$TIMESTAMP]

Changed: $CHANGED"

# Push (best-effort)
git push -u origin "$BRANCH" 2>/dev/null || git push 2>/dev/null || true

# Draft PR if feature branch and no open PR
if ! gh pr view "$BRANCH" --json number 2>/dev/null | grep -q '"number"'; then
  ISSUE_NUM=$(echo "$BRANCH" | grep -oE '[0-9]+' | head -1 || echo "")
  PR_TITLE=$(echo "$BRANCH" | sed 's|feat/||;s|fix/||;s|chore/||;s|-| |g')
  PR_BODY="Auto-draft PR for branch \`$BRANCH\`."
  [[ -n "$ISSUE_NUM" ]] && PR_BODY="$PR_BODY

Closes #$ISSUE_NUM"
  gh pr create \
    --title "$PR_TITLE" \
    --body "$PR_BODY" \
    --draft 2>/dev/null || true
fi

echo "[auto-commit] committed: $CHANGED"
