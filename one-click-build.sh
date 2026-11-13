#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="Destro0007/PromptForge-Android"
WORKFLOW="android.yml"

echo "======================================"
echo " PROMPTFORGE - ONE CLICK BUILD SYSTEM"
echo "======================================"

# Check tools and authentication
for tool in git gh python; do
  command -v "$tool" >/dev/null || {
    echo "Missing tool: $tool"
    exit 1
  }
done

gh auth status || exit 1

# Commit all current project changes
git add -A

if ! git diff --cached --quiet; then
  git commit -m "Automated PromptForge build update" || exit 1
fi

# Push to GitHub
git push origin main || exit 1

HEAD_SHA=$(git rev-parse HEAD)

# If no new commit was created, manually trigger CI
if [ -z "$(git log -1 --format=%s)" ]; then
  echo "No commit found"
  exit 1
fi

echo "Waiting for GitHub Actions..."

# Find the workflow run for this exact commit
RUN_ID=""
for attempt in $(seq 1 30); do
  RUN_ID=$(gh run list \
    --repo "$REPO" \
    --workflow "$WORKFLOW" \
    --branch main \
    --limit 10 \
    --json databaseId,headSha \
    | python -c 'import json,sys; sha=sys.argv[1]; runs=json.load(sys.stdin); print(next((str(x["databaseId"]) for x in runs if x["headSha"]==sha), ""))' "$HEAD_SHA")

  [ -n "$RUN_ID" ] && break
  sleep 5
done

# If there was no push-triggered run, dispatch one
if [ -z "$RUN_ID" ]; then
  echo "No automatic run found. Triggering workflow..."
  gh workflow run "$WORKFLOW" --repo "$REPO" --ref main || exit 1
  sleep 8

  RUN_ID=$(gh run list --repo "$REPO" \
    --workflow "$WORKFLOW" --branch main --limit 1 \
    --json databaseId \
    | python -c 'import json,sys; x=json.load(sys.stdin); print(x[0]["databaseId"] if x else "")')
fi

[ -n "$RUN_ID" ] || {
  echo "Could not find GitHub Actions run."
  exit 1
}

echo "Build ID: $RUN_ID"
echo "Waiting for build completion..."

if gh run watch "$RUN_ID" --repo "$REPO" --exit-status; then
  echo "BUILD SUCCESSFUL!"

  mkdir -p build-output
  gh run download "$RUN_ID" --repo "$REPO" \
    --dir build-output || exit 1

  echo "======================================"
  echo " APK DOWNLOAD COMPLETE"
  echo "======================================"
  find build-output -type f -name "*.apk" -print

  echo "APK location: $PWD/build-output/"
else
  echo "BUILD FAILED - collecting error logs..."

  gh run view "$RUN_ID" --repo "$REPO" \
    --log-failed > build-failure.log 2>&1 || true

  echo "Error report saved to:"
  echo "$PWD/build-failure.log"

  tail -n 80 build-failure.log
  exit 1
fi
