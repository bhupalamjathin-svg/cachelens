#!/usr/bin/env bash
# Paced live demo: press Enter to move to each step.
# Usage: scripts/live-demo.sh <service-url>     (URL from: minikube service cachelens --url)
set -uo pipefail
URL="${1:?usage: scripts/live-demo.sh <service-url>}"
cd "$(dirname "$0")/.."

step() { echo; printf '\033[1;36m==== %s ====\033[0m\n' "$1"; [ -n "${2:-}" ] && printf '\033[0;33m  say: %s\033[0m\n' "$2"; read -rp "  [Enter to run] " _; }
run()  { printf '\033[1;32m$ %s\033[0m\n' "$*"; eval "$@"; }

step "0. Pre-flight" "Cluster, Jenkins and the target server are all up."
run "kubectl get nodes"
run "docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'jenkins|cachelens-target|minikube'"

step "1. The app" "A cache-eviction simulator API: every write tells you what got evicted."
run "curl -s $URL/; echo"
run "curl -s -X PUT $URL/api/cache/demo -H 'Content-Type: application/json' -d '{\"value\":\"hello sir\"}'; echo"
run "curl -s $URL/api/stats; echo"

step "2. Git history" "Feature branches merged into main through pull requests."
run "git log --oneline --graph -12"

step "3. CI: push -> Jenkins builds automatically" "Open http://localhost:8090 now. I won't click Build."
run "git commit --allow-empty -m 'demo: live CI trigger' && git push"
echo "  -> Within ~60s a new build appears: 'Started by an SCM change'."
echo "     Show: Stage View (Test before Build), Test Result 8/8, artifact cachelens-1.0.N-$(git rev-parse --short HEAD).jar"

step "4. Ansible: configure a server" "The role installs Java, deploys the JAR as a systemd service. Handler restarts only on change."
CAP=$((RANDOM % 40 + 10))
run "(cd ansible && ansible-playbook -i inventory.ini playbook.yml -e cache_capacity=$CAP | tail -25)"
run "curl -s localhost:8081/; echo"

step "5. Kubernetes: 3 replicas behind a Service" "Each response names the pod that answered."
run "kubectl get deploy,pods,svc -l app=cachelens"
run "scripts/spread.sh $URL 20"

step "6. Scale 3 -> 5" "Two new pods appear within seconds."
run "kubectl scale deployment cachelens --replicas=5 && kubectl rollout status deployment/cachelens --timeout=120s"
run "kubectl get pods -l app=cachelens"

step "7. Scale back 5 -> 3"
run "kubectl scale deployment cachelens --replicas=3 && sleep 5 && kubectl get pods -l app=cachelens"

step "8. Probes + metrics" "Startup/liveness/readiness probes; Prometheus-format counters."
run "kubectl describe pod -l app=cachelens | grep -E 'Liveness|Readiness|Startup' | head -3"
run "curl -s $URL/actuator/prometheus | grep '^cachelens_'"

step "9. Bad deploy + rollback" "Broken config: new pod crashes, old pods keep serving, then undo."
run "scripts/rollback-demo.sh $URL"

echo; printf '\033[1;35m==== Demo complete: code -> build -> test -> configure -> deploy -> scale -> recover ====\033[0m\n'
