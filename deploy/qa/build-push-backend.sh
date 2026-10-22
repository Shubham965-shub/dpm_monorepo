#!/usr/bin/env bash
# Builds the backend fast-jar (unit tests included) and publishes it to the QA registry as
# dpm-backend:<tag>. Shared by provision.sh (first image) and the backend workflow, so the build is
# defined exactly once. The image is built by ACR Tasks (az acr build) from the unchanged
# src/main/docker/Dockerfile.jvm - no local Docker daemon needed on a laptop or a CI runner.
#
# Usage: build-push-backend.sh [tag]   (default tag: 12-char git sha)
# Prints the full image reference last; also writes image=<ref> to $GITHUB_OUTPUT when set.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
source "$here/env.sh"
backend_dir="$(cd "$here/../../backend" && pwd)"

tag="${1:-$(git -C "$backend_dir" rev-parse --short=12 HEAD)}"
image="$ACR_NAME.azurecr.io/$BACKEND_IMAGE_REPO:$tag"

cd "$backend_dir"

# run-docker.sh leaves a proxy-patched JDK trust store here on laptops behind a TLS-inspecting
# corporate proxy; reuse it so Maven can resolve dependencies. Absent on CI runners, so a no-op there.
patched_cacerts="$backend_dir/.devtools/cacerts-with-zscaler"
if [ -z "${MAVEN_OPTS:-}" ] && [ -f "$patched_cacerts" ]; then
  export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$patched_cacerts -Djavax.net.ssl.trustStorePassword=changeit"
fi

echo "--- Building jar (unit tests run) ---"
./mvnw -B -ntp package

echo "--- Building and pushing $image via ACR Tasks ---"
az acr build --subscription "$AZ_SUBSCRIPTION_ID" --registry "$ACR_NAME" \
  --image "$BACKEND_IMAGE_REPO:$tag" --file src/main/docker/Dockerfile.jvm .

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "image=$image" >> "$GITHUB_OUTPUT"
fi
echo "$image"
