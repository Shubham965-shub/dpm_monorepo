#!/usr/bin/env bash
# Every name, id, and sizing value for the QA deployment lives here. Sourced by the other scripts
# in this directory (and, through them, by the GitHub workflows) so a rename happens in one place.
# Decisions and rationale: docs/superpowers/specs/2026-09-22-qa-azure-deployment-design.md

export AZ_SUBSCRIPTION_ID="16434d97-76c8-4357-b11d-05e42e1e1e49"   # STRIDES-RISE-LH-01 (QA)
export AZ_TENANT_ID="9c70483a-4e14-4e1e-862a-5fda2a0a5d77"
export AZ_RESOURCE_GROUP="QA-Lighthouse"
export AZ_LOCATION="centralindia"
# Static Web Apps is not offered in Central India; every sibling QA SWA in this RG uses East Asia.
export AZ_SWA_LOCATION="eastasia"

export ACR_NAME="dpmqaacr"
export LAW_NAME="dpm-qa-law"
export CAE_NAME="dpm-qa-env"
export BACKEND_APP_NAME="dpm-backend-qa"
export BACKEND_IMAGE_REPO="dpm-backend"
export SWA_NAME="dpm-frontend-qa"
# Standard, not Free: this subscription already holds the maximum number of Free static sites
# (creation fails with "too many static sites with SKU: Free"), same reason sop-stp-fe-qa is Standard.
export SWA_SKU="Standard"
export GITHUB_IDENTITY_NAME="dpm-qa-github-deploy"

export GITHUB_REPO="Strides-Pharma-Science-Ltd/dpm_monorepo"
export GITHUB_ENVIRONMENT="qa"

# Backend container sizing (chosen 2026-09-22: small always-on API, no cold starts).
export BACKEND_CPU="0.5"
export BACKEND_MEMORY="1.0Gi"
export BACKEND_MIN_REPLICAS=1
export BACKEND_MAX_REPLICAS=1
export BACKEND_PORT=8080

# Rollout wait in deploy-backend.sh. Proposed starter values (a JVM Quarkus app is typically
# serving within ~30s; 5 minutes leaves room for a slow image pull) - adjust if rollouts prove slower.
export BACKEND_ROLLOUT_TIMEOUT_SECONDS=300
export BACKEND_ROLLOUT_POLL_SECONDS=10
