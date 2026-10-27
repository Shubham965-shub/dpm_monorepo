#!/usr/bin/env bash
# Reports a CI build (and optionally its test coverage) to the feedback-service CI dashboard
# (apps/feedback-service `com.strides.feedback.ci` module), so the admin UI's "CI / Builds"
# page shows what GitHub Actions just did.
#
# Required env:
#   JWT_SECRET            shared HS256 secret the deployed feedback-service verifies tokens with
#                          (same value as that service's JWT_SECRET env var). If unset, this
#                          script logs a warning and exits 0 — forks/PRs from outside the org
#                          don't have the secret and shouldn't fail CI over a reporting step.
#   CI_API_BASE_URL        e.g. https://feedback-service.example.com/api/v1 (no trailing /admin/ci)
#   BUILD_STATUS            SUCCESS | FAILED | CANCELLED | RUNNING | QUEUED
#   WORKFLOW_NAME
#   STARTED_AT / COMPLETED_AT   ISO-8601 UTC, e.g. 2026-10-22T10:00:00Z
#
# Optional env (coverage — only sent when COVERAGE_COMPONENT is set):
#   COVERAGE_COMPONENT     FRONTEND | BACKEND | QA
#   COVERAGE_PERCENT       e.g. 82.35
#   COVERAGE_THRESHOLD     default 80
#   TESTS_TOTAL / TESTS_PASSED / TESTS_FAILED / TESTS_SKIPPED
#   COVERAGE_DURATION_SECONDS
#
# Uses GitHub Actions' own default env vars (GITHUB_REPOSITORY, GITHUB_RUN_ID, GITHUB_RUN_NUMBER,
# GITHUB_SHA, GITHUB_REF_NAME, GITHUB_HEAD_REF, GITHUB_ACTOR, GITHUB_SERVER_URL) for the rest.
#
# Works identically for private/internal repos and org-owned repos, with NO extra config — this
# script never calls the GitHub API. It only reads values GitHub Actions already injects into its
# own runner (regardless of visibility/ownership) and POSTs them outbound to feedback-service over
# plain HTTPS, authenticated with the JWT_SECRET above. The only requirement is the same for every
# repo: add JWT_SECRET as a secret on that repo. (Contrast with the separate "File as GitHub
# Issue" feature in feedback-service, which DOES call the GitHub API and does need a properly
# scoped PAT for private/org repos — see GitHubIssueService's docs.)

set -euo pipefail

# Reporting is observability, not a gate on the build: whenever it can't happen (no secret, the
# dashboard is unreachable, or the deployed service rejects the call) warn and exit 0. No coverage
# was submitted, so the threshold gate has nothing to check — report it as SKIPPED.
skip_reporting() {
  echo "::warning::$1 — skipping CI dashboard reporting (the build itself is unaffected)."
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "coverage_status=SKIPPED" >> "$GITHUB_OUTPUT"
  fi
  exit 0
}

if [ -z "${JWT_SECRET:-}" ]; then
  # Expected for forked-repo PRs. On a private/org repo, add JWT_SECRET under
  # Settings → Secrets and variables → Actions to turn reporting on.
  skip_reporting "JWT_SECRET is not set"
fi

CI_API_BASE_URL="${CI_API_BASE_URL:-https://feedback-service.graysand-fe5571c1.centralindia.azurecontainerapps.io/api/v1}"
CI_BASE="${CI_API_BASE_URL%/}/admin/ci"

OWNER="${GITHUB_REPOSITORY%%/*}"
REPO_NAME="${GITHUB_REPOSITORY##*/}"
BRANCH="${GITHUB_HEAD_REF:-$GITHUB_REF_NAME}"
RUN_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
# A fixed, documented "service account" subject — AuthFilter only requires a valid-UUID `sub`
# claim; it doesn't look the id up anywhere. Identifies the caller as the CI pipeline itself.
CI_SERVICE_SUBJECT="00000000-0000-4000-8000-000000000000"

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

mint_jwt() {
  local header payload header_b64 payload_b64 signing_input signature now exp
  header='{"alg":"HS256","typ":"JWT"}'
  now=$(date +%s)
  exp=$((now + 600)) # 10 minutes — plenty for one workflow run, short-lived since this shares the real auth secret
  payload=$(jq -nc --arg sub "$CI_SERVICE_SUBJECT" --argjson iat "$now" --argjson exp "$exp" \
    '{sub: $sub, roles: ["Admin"], name: "GitHub Actions", iat: $iat, exp: $exp}')
  header_b64=$(printf '%s' "$header" | b64url)
  payload_b64=$(printf '%s' "$payload" | b64url)
  signing_input="${header_b64}.${payload_b64}"
  signature=$(printf '%s' "$signing_input" | openssl dgst -binary -sha256 -hmac "$JWT_SECRET" | b64url)
  printf '%s.%s' "$signing_input" "$signature"
}

TOKEN=$(mint_jwt)

# Prints the response body on 2xx. Otherwise prints why to stderr (so it isn't captured) and
# returns 1 — unlike `curl -f`, which hides the server's error message.
api_post() {
  local path="$1" body="$2" response status hint=""
  if ! response=$(curl -sS -X POST "${CI_BASE}${path}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${body}" \
    -w $'\n%{http_code}'); then
    echo "Could not reach ${CI_BASE}${path}" >&2
    return 1
  fi
  status="${response##*$'\n'}"
  response="${response%$'\n'*}"
  if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
    printf '%s' "$response"
    return 0
  fi
  case "$status" in
    401|403) hint=" (does this repo's JWT_SECRET match the deployed feedback-service's JWT_SECRET?)" ;;
    404) hint=" (is the CI module deployed to ${CI_API_BASE_URL}?)" ;;
  esac
  echo "POST ${path} returned HTTP ${status}${hint}: ${response:0:300}" >&2
  return 1
}

echo "Ensuring CI project is registered for ${GITHUB_REPOSITORY}..."
PROJECT_BODY=$(jq -nc --arg name "$REPO_NAME" --arg owner "$OWNER" --arg repo "$REPO_NAME" \
  '{name: $name, github_owner: $owner, github_repo: $repo}')
api_post "/projects" "$PROJECT_BODY" > /dev/null \
  || skip_reporting "Could not register the CI project"

echo "Reporting build #${GITHUB_RUN_NUMBER} (run ${GITHUB_RUN_ID}, status ${BUILD_STATUS}) for ${GITHUB_REPOSITORY}..."
BUILD_BODY=$(jq -nc \
  --argjson github_run_id "$GITHUB_RUN_ID" \
  --argjson build_number "$GITHUB_RUN_NUMBER" \
  --arg repository "$GITHUB_REPOSITORY" \
  --arg workflow_name "${WORKFLOW_NAME:-CI}" \
  --arg branch "$BRANCH" \
  --arg commit_sha "$GITHUB_SHA" \
  --arg commit_short_sha "${GITHUB_SHA:0:7}" \
  --arg status "$BUILD_STATUS" \
  --arg environment "ci" \
  --arg triggered_by "$GITHUB_ACTOR" \
  --arg performed_by "$GITHUB_ACTOR" \
  --arg run_url "$RUN_URL" \
  --arg started_at "${STARTED_AT:-}" \
  --arg completed_at "${COMPLETED_AT:-}" \
  '{
    github_run_id: $github_run_id,
    build_number: $build_number,
    repository: $repository,
    workflow_name: $workflow_name,
    branch: $branch,
    commit_sha: $commit_sha,
    commit_short_sha: $commit_short_sha,
    status: $status,
    environment: $environment,
    triggered_by: $triggered_by,
    performed_by: $performed_by,
    run_url: $run_url
  }
  + (if $started_at != "" then {started_at: $started_at} else {} end)
  + (if $completed_at != "" then {completed_at: $completed_at} else {} end)')

BUILD_RESPONSE=$(api_post "/builds" "$BUILD_BODY") \
  || skip_reporting "Could not report the build"
BUILD_ID=$(echo "$BUILD_RESPONSE" | jq -r '.id')
echo "Build id: ${BUILD_ID}"

if [ -n "${COVERAGE_COMPONENT:-}" ]; then
  echo "Reporting ${COVERAGE_COMPONENT} coverage: ${COVERAGE_PERCENT:-0}%..."
  COVERAGE_BODY=$(jq -nc \
    --arg component "$COVERAGE_COMPONENT" \
    --argjson coverage_percent "${COVERAGE_PERCENT:-0}" \
    --argjson coverage_threshold "${COVERAGE_THRESHOLD:-80}" \
    --argjson tests_total "${TESTS_TOTAL:-0}" \
    --argjson tests_passed "${TESTS_PASSED:-0}" \
    --argjson tests_failed "${TESTS_FAILED:-0}" \
    --argjson tests_skipped "${TESTS_SKIPPED:-0}" \
    --arg duration_seconds "${COVERAGE_DURATION_SECONDS:-}" \
    '{
      component: $component,
      coverage_percent: $coverage_percent,
      coverage_threshold: $coverage_threshold,
      tests_total: $tests_total,
      tests_passed: $tests_passed,
      tests_failed: $tests_failed,
      tests_skipped: $tests_skipped
    }
    + (if $duration_seconds != "" then {duration_seconds: ($duration_seconds | tonumber)} else {} end)')

  COVERAGE_RESPONSE=$(api_post "/builds/${BUILD_ID}/coverage" "$COVERAGE_BODY") \
    || skip_reporting "Could not report ${COVERAGE_COMPONENT} coverage"
  COVERAGE_STATUS=$(echo "$COVERAGE_RESPONSE" | jq -r '.coverage_status')
  echo "Coverage reported — status: ${COVERAGE_STATUS} (threshold ${COVERAGE_THRESHOLD:-80}%)."
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "coverage_status=${COVERAGE_STATUS}" >> "$GITHUB_OUTPUT"
  fi
elif [ -n "${GITHUB_OUTPUT:-}" ]; then
  # No coverage reported this run — nothing for the threshold gate to fail on.
  echo "coverage_status=SKIPPED" >> "$GITHUB_OUTPUT"
fi

echo "Done — see the CI / Builds tab in feedback-admin for ${GITHUB_REPOSITORY}."
