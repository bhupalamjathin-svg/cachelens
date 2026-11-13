#!/usr/bin/env bash
# Rollback demo:
#  1. ship a broken config (CACHE_CAPACITY=0 -> app refuses to start, fails validation)
#  2. show the rollout stall: new pod CrashLoops, but the 3 old pods keep serving (maxUnavailable: 0 + readiness probe)
#  3. kubectl rollout undo -> back to the last good revision
set -uo pipefail
URL="${1:?usage: scripts/rollback-demo.sh <service-url>}"

echo "### Current revision history"; kubectl rollout history deployment/cachelens

echo; echo "### Deploying BROKEN change: CACHE_CAPACITY=0"
kubectl set env deployment/cachelens CACHE_CAPACITY=0
kubectl annotate deployment/cachelens kubernetes.io/change-cause="v2 - BROKEN: CACHE_CAPACITY=0" --overwrite
kubectl rollout status deployment/cachelens --timeout=45s || echo ">>> Rollout did NOT complete (expected)"

echo; echo "### Pods during the bad rollout"; kubectl get pods -l app=cachelens
NEW_POD="$(kubectl get pods -l app=cachelens --sort-by=.metadata.creationTimestamp -o name | tail -1)"
echo; echo "### Why the new pod fails ($NEW_POD)"; kubectl logs "$NEW_POD" --tail=200 2>/dev/null | grep -iE "capacity|APPLICATION FAILED|must be" | head -5
echo; echo "### Is the app still up? (old pods still serving)"; curl -s "$URL/" ; echo

echo; echo "### ROLLING BACK"
kubectl rollout undo deployment/cachelens
kubectl annotate deployment/cachelens kubernetes.io/change-cause="v1 - restored by rollout undo" --overwrite
kubectl rollout status deployment/cachelens --timeout=120s

echo; echo "### After rollback"; kubectl get pods -l app=cachelens; kubectl rollout history deployment/cachelens
curl -s "$URL/"; echo
