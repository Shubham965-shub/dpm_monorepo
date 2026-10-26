#!/usr/bin/env bash
# Reads one component's coverage + test counts from the files the coverage workflows already
# produce, then hands them to report-ci.sh, which posts them to the feedback-service CI dashboard
# (feedback-admin → "CI / Builds"). All three coverage jobs run inside the same workflow run, so
# they share GITHUB_RUN_ID and land on one dashboard build with FRONTEND, BACKEND and QA cards.
#
# Usage: report-coverage.sh <FRONTEND|BACKEND|QA> <module dir>
#   BACKEND / QA  read <dir>/target/site/jacoco/jacoco.csv (line coverage) and surefire reports
#   FRONTEND      reads <dir>/coverage/coverage-summary.json (lines.pct) and <dir>/test-results.json
#
# Env: BUILD_STATUS (SUCCESS|FAILED), STARTED_AT, plus everything report-ci.sh needs (JWT_SECRET,
# CI_API_BASE_URL). Never fails the job — a missing report is sent as 0%.

set -euo pipefail

COMPONENT="$1"
DIR="$2"
PERCENT="0"; TOTAL=0; FAILED=0; SKIPPED=0; PASSED=0

if [ "$COMPONENT" = "FRONTEND" ]; then
  if [ -f "$DIR/coverage/coverage-summary.json" ]; then
    PERCENT=$(jq '.total.lines.pct' "$DIR/coverage/coverage-summary.json")
  else
    echo "::warning::No coverage-summary.json in $DIR — reporting 0% $COMPONENT coverage."
  fi
  if [ -f "$DIR/test-results.json" ]; then
    TOTAL=$(jq '.numTotalTests' "$DIR/test-results.json")
    PASSED=$(jq '.numPassedTests' "$DIR/test-results.json")
    FAILED=$(jq '.numFailedTests' "$DIR/test-results.json")
    SKIPPED=$(jq '.numPendingTests' "$DIR/test-results.json")
  fi
else
  CSV="$DIR/target/site/jacoco/jacoco.csv"
  if [ -f "$CSV" ]; then
    # Columns 8/9 are LINE_MISSED/LINE_COVERED.
    PERCENT=$(awk -F',' 'NR>1 {m+=$8; c+=$9} END {t=m+c; printf "%.2f", (t > 0 ? c / t * 100 : 0)}' "$CSV")
  else
    echo "::warning::No jacoco.csv in $DIR — reporting 0% $COMPONENT coverage."
  fi
  shopt -s nullglob
  for f in "$DIR"/target/surefire-reports/*.txt; do
    LINE=$(grep "Tests run:" "$f" | tail -1 || true)
    T=$(echo "$LINE" | sed -n 's/.*Tests run: \([0-9]*\).*/\1/p')
    F=$(echo "$LINE" | sed -n 's/.*Failures: \([0-9]*\).*/\1/p')
    E=$(echo "$LINE" | sed -n 's/.*Errors: \([0-9]*\).*/\1/p')
    S=$(echo "$LINE" | sed -n 's/.*Skipped: \([0-9]*\).*/\1/p')
    TOTAL=$((TOTAL + ${T:-0}))
    FAILED=$((FAILED + ${F:-0} + ${E:-0}))
    SKIPPED=$((SKIPPED + ${S:-0}))
  done
  PASSED=$((TOTAL - FAILED - SKIPPED))
fi

echo "$COMPONENT: ${PERCENT}% line coverage, ${PASSED}/${TOTAL} tests passed"

COMPLETED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
COVERAGE_COMPONENT="$COMPONENT" \
COVERAGE_PERCENT="$PERCENT" \
TESTS_TOTAL="$TOTAL" TESTS_PASSED="$PASSED" TESTS_FAILED="$FAILED" TESTS_SKIPPED="$SKIPPED" \
  bash "$(dirname "$0")/report-ci.sh"
