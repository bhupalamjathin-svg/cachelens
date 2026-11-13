#!/usr/bin/env bash
# Exercise every endpoint once. Usage: scripts/smoke-test.sh [BASE_URL]   (default http://localhost:8080)
set -euo pipefail
BASE="${1:-http://localhost:8080}"
j() { if command -v jq >/dev/null; then jq -c .; else cat; echo; fi; }

echo "== GET /";                          curl -s "$BASE/" | j
for k in alpha beta gamma; do
  echo "== PUT /api/cache/$k";            curl -s -X PUT "$BASE/api/cache/$k" -H 'Content-Type: application/json' -d "{\"value\":\"$k-value\"}" | j
done
echo "== GET /api/cache/alpha (hit)";     curl -s "$BASE/api/cache/alpha" | j
echo "== GET /api/cache/zeta (miss)";     curl -s "$BASE/api/cache/zeta" | j
echo "== GET /api/stats";                 curl -s "$BASE/api/stats" | j
echo "== GET /actuator/health";           curl -s "$BASE/actuator/health" | j
