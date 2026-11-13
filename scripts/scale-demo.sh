#!/usr/bin/env bash
# 3 -> 5 -> 3 replicas, printing pods at each step.
set -euo pipefail
show() { echo; echo "### $1"; kubectl get deployment cachelens; kubectl get pods -l app=cachelens -o wide; }
show "Before scaling (3 replicas)"
kubectl scale deployment cachelens --replicas=5
kubectl rollout status deployment/cachelens --timeout=120s
show "After scale-up (5 replicas)"
kubectl scale deployment cachelens --replicas=3
sleep 5
show "After scale-down (3 replicas)"
