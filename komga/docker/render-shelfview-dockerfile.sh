#!/usr/bin/env bash
# SV-701: render a buildable, hardened ShelfView Dockerfile from the upstream
# JReleaser template (komga/docker/Dockerfile.tpl).
#
# Keeping the additions in this script (instead of editing Dockerfile.tpl) keeps
# the upstream template untouched and preserves upstream merge-ability.
#
# Usage:
#   render-shelfview-dockerfile.sh <template> <jar-file-name> <output>
#
# The rendered Dockerfile is built with build args:
#   --build-arg SHELFVIEW_VERSION=<version> --build-arg SHELFVIEW_REVISION=<git-sha>
set -euo pipefail

template="${1:?usage: render-shelfview-dockerfile.sh <template> <jar-file-name> <output>}"
jar_name="${2:?missing jar file name}"
output="${3:?missing output path}"

# 1) Replace the JReleaser artifact placeholder with the real boot jar name.
sed "s|{{distributionArtifactFile}}|${jar_name}|g" "$template" > "$output"

# 2) Append ShelfView hardening to the final (runner) stage:
#    - non-root execution as UID/GID 1000
#    - container health check against the unauthenticated actuator endpoint
#      (SecurityConfiguration permits /actuator/health without auth)
#    - version / revision OCI labels supplied at build time
cat >> "$output" <<'DOCKERFILE'

# --- ShelfView additions (SV-701): non-root, health check, version labels ---
ARG SHELFVIEW_VERSION=dev
ARG SHELFVIEW_REVISION=unknown
RUN mkdir -p /config && chown -R 1000:1000 /app /config
USER 1000:1000
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://localhost:25600/actuator/health || exit 1
LABEL org.opencontainers.image.title="ShelfView" \
      org.opencontainers.image.description="Self-hosted web reader based on Komga (Web-Hanji/ShelfView fork)" \
      org.opencontainers.image.version="${SHELFVIEW_VERSION}" \
      org.opencontainers.image.revision="${SHELFVIEW_REVISION}"
DOCKERFILE

echo "Rendered hardened Dockerfile -> $output"
