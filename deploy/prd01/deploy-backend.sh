#!/usr/bin/env bash
# Deploy the PRD01 backend (helios-backend-lhprd - Quarkus JVM).
#
# The backend image is env-agnostic (all config is runtime app settings), so the exact image the QA
# CI built for this SHA (dpm-backend:<sha> in the QA registry) is promoted to prod unchanged. Build
# it first by merging the commit into `qa`, then run this.
#
# Usage:  deploy/prd01/deploy-backend.sh [tag]   # tag defaults to $TAG or the current commit's SHA
#   SKIP_ROLL=1  import only (first provisioning, before 06-appservice.sh has created the app)
#   SKIP_PROBE=1 skip the probe (e.g. off the corp network)
set -euo pipefail
source "$(dirname "$0")/lib-deploy.sh"

need az; need git; need curl
check_az
TAG="$(resolve_tag "${1:-}")"
info "backend deploy - ${BACKEND_APP} @ ${TAG}"

import_to_prod "$QA_BACKEND_IMAGE" "$BACKEND_IMAGE" "$TAG"
[ "${SKIP_ROLL:-0}" = "1" ] && { info "SKIP_ROLL=1 - image imported, app not rolled"; exit 0; }
roll_webapp "$BACKEND_APP" "$BACKEND_IMAGE" "$TAG"

# The backend has no public route of its own; a GET on the POST-only login route returning 405
# through the frontend proxy proves the backend is up and routing.
probe "/api/auth/login" "405" "backend up (via /api proxy)"
info "backend now on ${BACKEND_IMAGE}:${TAG} - record it in deploy/prd01/RELEASES.md"
