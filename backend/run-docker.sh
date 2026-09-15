#!/usr/bin/env bash
# Builds and runs the backend in a Docker container via OrbStack. Run from the backend/ directory
# (or anywhere - the script cd's itself). Requires .env to be filled in (see README.md).
#
# Why a container instead of `mvnw quarkus:dev` directly: on this machine, direct JVM connections
# to the remote Azure Postgres over this network intermittently reset after the first query
# (observed directly, cause not fully diagnosed - likely something in how the corporate-proxied
# network path handles long-lived JVM socket connections). Routing through OrbStack's container
# network has been reliable in practice. If you're on a network without that issue, plain
# `./mvnw quarkus:dev` works fine too and gives you live-reload.
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "backend/.env not found - copy the template from README.md and fill in DB_JDBC_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET first." >&2
  exit 1
fi

# --- One-time trust store patch, only needed if you're behind a TLS-inspecting corporate proxy
# (e.g. Zscaler) that Maven's JVM doesn't trust by default, even though curl/the OS does. Detects
# this automatically and only patches if needed - safe to run on a network without this issue too.
CACERTS_PATCHED=".devtools/cacerts-with-zscaler"
if [ ! -f "$CACERTS_PATCHED" ]; then
  JDK_HOME="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.home/{print $2}')"
  if echo | openssl s_client -connect repo.maven.apache.org:443 2>/dev/null | openssl x509 -noout -issuer 2>/dev/null | grep -qi zscaler; then
    echo "Corporate TLS-inspecting proxy detected - patching a local JDK trust store copy (one-time)..."
    mkdir -p .devtools
    security find-certificate -a -c "Zscaler" -p /Library/Keychains/System.keychain > /tmp/zscaler-root-ca.pem
    cp "$JDK_HOME/lib/security/cacerts" "$CACERTS_PATCHED"
    chmod u+w "$CACERTS_PATCHED"
    "$JDK_HOME/bin/keytool" -importcert -noprompt -trustcacerts \
      -alias zscaler-root-ca -file /tmp/zscaler-root-ca.pem \
      -keystore "$CACERTS_PATCHED" -storepass changeit
  fi
fi
if [ -f "$CACERTS_PATCHED" ]; then
  export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$(pwd)/$CACERTS_PATCHED -Djavax.net.ssl.trustStorePassword=changeit"
fi

echo "--- Building jar ---"
./mvnw -q clean package -DskipTests

echo "--- Building image ---"
docker build -f src/main/docker/Dockerfile.jvm -t dpm-backend:local .

echo "--- Running (http://localhost:8080) ---"
docker rm -f dpm-backend >/dev/null 2>&1 || true
docker run -d --name dpm-backend -p 8080:8080 --env-file .env dpm-backend:local

echo "Started. Logs:  docker logs -f dpm-backend"
echo "Stop:           docker stop dpm-backend"
