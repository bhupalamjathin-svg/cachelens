#!/usr/bin/env bash
# Hit the Service N times and count which pod answered each request -> proves load-balancing across replicas.
# Usage: scripts/spread.sh [BASE_URL] [N]     e.g. scripts/spread.sh "$(minikube service cachelens --url)" 30
set -euo pipefail
BASE="${1:?pass the service URL}"; N="${2:-30}"
for _ in $(seq "$N"); do
  # new connection per request so kube-proxy can pick a different pod each time
  curl -s -H 'Connection: close' "$BASE/" | sed -E 's/.*"instance":"([^"]+)".*/\1/'
done | sort | uniq -c | sort -rn
