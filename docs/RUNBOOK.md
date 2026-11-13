# CacheLens Runbook: start to finish, with the screenshots to capture

Run everything from the repo root on Linux, macOS or **WSL2** (Ubuntu). 📸 marks a screenshot for the report.

Tools: Git, JDK 17, Maven 3.9, Docker, Ansible (`pip install ansible` gives you `community.docker`), Minikube, kubectl, `jq` (optional).

---

## COMPONENT 1: Git, Maven, Jenkins

### 1. Prove the app works by itself
```bash
mvn clean verify                      # expect: Tests run: 8, Failures: 0 ... BUILD SUCCESS
java -jar target/cachelens-1.0.0-SNAPSHOT.jar
# in a 2nd terminal:
scripts/smoke-test.sh                 # 📸 responses incl. "evicted" and a 404 miss
```

### 2. Git: build real branch/merge history
Create an **empty** GitHub repo called `cachelens` (no README), then:
```bash
git init -b main
git add pom.xml src .gitignore README.md
git commit -m "feat: CacheLens cache-eviction API (LRU/LFU) with JUnit tests"
git remote add origin https://github.com/<you>/cachelens.git
git push -u origin main

git checkout -b feature/ci-pipeline
git add Jenkinsfile .github jenkins
git commit -m "ci: Jenkins pipeline (test stage, versioned artifact) + GitHub Actions"
git push -u origin feature/ci-pipeline
```
On GitHub, open a **Pull Request** feature/ci-pipeline → main and **merge** it. Then:
```bash
git checkout main && git pull
git checkout -b feature/deploy
git add Dockerfile .dockerignore ansible k8s scripts docs
git commit -m "deploy: Ansible role, multi-stage Dockerfile, k8s manifests with probes/ConfigMap"
git push -u origin feature/deploy        # PR -> merge again
git checkout main && git pull
git log --oneline --graph --all          # 📸 branches + merge commits
```
Also add the course coordinator as a collaborator (Settings → Collaborators) and put the URL in `repo_link.txt`.

### 3. Jenkins
```bash
docker compose -f jenkins/docker-compose.yml up -d --build
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
1. Open http://localhost:8090, install the **suggested plugins**, create an admin user.
2. **Manage Jenkins → Tools → Maven installations → Add**, name it **`Maven3`**, tick *Install automatically*. Save.
3. **New Item → `cachelens` → Pipeline**.
   - *Build Triggers*: tick **Poll SCM**, schedule `* * * * *` (the Jenkinsfile also sets this after the first run).
   - *Pipeline*: **Pipeline script from SCM** → Git → your repo URL → branch `*/main` → Script Path `Jenkinsfile`.
     (Private repo? Add a GitHub username + personal-access-token credential here.)
   - 📸 the config page (SCM + trigger + script path).
4. Click **Build Now** once. The first build registers the poll trigger.
5. **Prove CI is automatic**: make a small change and push it, e.g.
   ```bash
   echo "" >> README.md && git commit -am "docs: trigger CI" && git push
   ```
   Within about a minute a new build starts by itself, labelled "Started by an SCM change". 📸 that.
6. 📸 **Console Output** of a green build (Test → Build → Docker Image stages, `BUILD SUCCESS`).
7. 📸 the build page showing **Build Artifacts: `cachelens-1.0.N-<sha>.jar`** and the **Test Result** (8 passed).
8. 📸 the **Stage View**, which shows the Test stage running before Build.

### 4. CI enhancement evidence
- **Tests in CI**: Test Result page (8/8) and the Test stage in Stage View. 📸
- **Versioning**: artifact name `cachelens-1.0.<BUILD_NUMBER>-<git sha>.jar`, build name `#N 1.0.N-sha`, and
  `docker images cachelens` showing the same tag. 📸 Check the sha matches `git log -1 --format=%h`.
- **GitHub Actions**: repo → *Actions* tab → green "CI" run with the uploaded artifact. 📸
- Optional "failing test" demo: break an assertion, push, and show the pipeline go **red at Test** with Build skipped, then revert.

---

## COMPONENT 2: Ansible, Docker, Kubernetes

### 5. Ansible
```bash
mvn clean package                                   # the role deploys target/cachelens-*.jar
docker build -t cachelens-target ansible/target
docker run -d --name cachelens-target --privileged --cgroupns=host \
  -v /sys/fs/cgroup:/sys/fs/cgroup:rw -p 8081:8080 cachelens-target

cd ansible
ansible-galaxy collection install community.docker  # skip if you installed the full "ansible" package
ansible-playbook -i inventory.ini playbook.yml      # 📸 PLAY RECAP: failed=0, plus the "Report" line
ansible-playbook -i inventory.ini playbook.yml      # 📸 2nd run: changed=0, handler NOT triggered (idempotent)
cd ..
curl -s localhost:8081/ | jq                        # 📸 policy LFU, capacity 10 (from group_vars)
```
**Handler demo**: run `ansible-playbook -i inventory.ini playbook.yml -e cache_capacity=20` and show
`RUNNING HANDLER [cachelens : Restart cachelens]` fire because the env file changed. 📸

Role structure for the report: `tree ansible` 📸. The role was scaffolded with `ansible-galaxy init roles/cachelens`.
If your examiner wants to see you run that command, run it in a scratch folder and screenshot it.

> If the systemd container won't boot on your machine (some Docker Desktop setups), use any Ubuntu VM
> (Multipass/VirtualBox) and the SSH line in `inventory.ini` instead. The playbook doesn't change.

### 6. Docker (multi-stage)
```bash
docker build -t cachelens:local .
docker images cachelens                               # 📸 note the runtime image is JRE-only
docker run --rm -d -p 8080:8080 --name cl cachelens:local && sleep 8 && scripts/smoke-test.sh && docker stop cl
```
For the multi-stage enhancement, 📸 the build log showing `[build 5/5]` and the final stage, and compare sizes:
`docker images maven:3.9-eclipse-temurin-17` (build tools) vs `cachelens:local`.

### 7. Kubernetes
```bash
minikube start --memory=4096 --cpus=2
eval $(minikube docker-env)              # build straight into Minikube's Docker, no registry needed
docker build -t cachelens:v1 .

kubectl apply -f k8s/
kubectl rollout status deployment/cachelens
kubectl get pods -o wide                 # 📸 3/3 Running
kubectl get svc cachelens                # 📸 NodePort 30080
URL=$(minikube service cachelens --url)
scripts/smoke-test.sh "$URL"             # 📸 app responding through the Service
scripts/spread.sh "$URL" 30              # 📸 requests spread across 3 pod names
```
(On Docker-driver Minikube on Windows/macOS, `minikube service cachelens --url` keeps a tunnel open. Run it in its
own terminal and use the URL it prints.)

### 8. Scaling
```bash
scripts/scale-demo.sh                    # 📸 3 pods, then 5, then 3
# or by hand: kubectl scale deployment cachelens --replicas=5 ; kubectl get pods -w
scripts/spread.sh "$URL" 40              # 📸 (while at 5) traffic across 5 pods
```

### 9. Deployment enhancements: evidence
**Rollback (main demo):**
```bash
scripts/rollback-demo.sh
```
📸 in order: the new pod in `CrashLoopBackOff`, its log line `cachelens.capacity ... must be greater than or equal to 1`,
`curl` still answering from the **old** pods (zero downtime thanks to `maxUnavailable: 0` + readiness probe),
`rollout undo`, then 3/3 Running and `kubectl rollout history`.

**ConfigMap:**
```bash
kubectl get configmap cachelens-config -o yaml        # 📸
kubectl patch configmap cachelens-config -p '{"data":{"CACHE_POLICY":"LFU"}}'
kubectl rollout restart deployment/cachelens && kubectl rollout status deployment/cachelens
curl -s "$URL/" | jq .policy                           # 📸 "LFU", with no image rebuild
```
**Probes:** `kubectl describe pod <pod> | grep -A3 -E "Liveness|Readiness|Startup"` 📸

**Metrics:** `curl -s "$URL/actuator/prometheus" | grep cachelens_` 📸. For the report: Prometheus would scrape this
endpoint every 15s and track `rate(cachelens_hits_total[1m]) / (rate(cachelens_hits_total[1m]) + rate(cachelens_misses_total[1m]))`
as the live hit ratio per policy, and alert if it drops.

**Versioned image from Jenkins into Kubernetes (ties both components together):**
```bash
eval $(minikube docker-env); docker build --build-arg REVISION=1.0.7-abc1234 -t cachelens:1.0.7-abc1234 .
kubectl set image deployment/cachelens cachelens=cachelens:1.0.7-abc1234
kubectl annotate deployment/cachelens kubernetes.io/change-cause="1.0.7-abc1234 from Jenkins build #7" --overwrite
curl -s "$URL/" | jq .version             # 📸 shows 1.0.7-abc1234
```

Optional bonus (HPA): `minikube addons enable metrics-server && kubectl apply -f k8s/extras/hpa.yaml && kubectl get hpa`.

---

## Clean-up
```bash
kubectl delete -f k8s/ ; minikube stop
docker rm -f cachelens-target
docker compose -f jenkins/docker-compose.yml down
```

## Resume lines
**CacheLens — CI/CD Pipeline & Kubernetes Deployment** | github.com/<you>/cachelens
*Java 17, Spring Boot, Maven, Git, Jenkins, GitHub Actions, Docker, Ansible, Kubernetes*
- Built a cache-eviction simulator REST API (LRU/LFU, hit-rate and eviction metrics) with a Jenkins pipeline triggered on every Git push that runs JUnit tests before packaging.
- Versioned every artifact and Docker image by build number and commit SHA, and mirrored the pipeline in GitHub Actions.
- Automated server provisioning with an Ansible role (variables, templated systemd unit, change-triggered restart handler).
- Deployed on Kubernetes with 3→5 replicas behind a Service, ConfigMap-driven config, and startup/liveness/readiness probes, using a multi-stage Docker image.
- Demonstrated a zero-downtime failed rollout and recovery with `kubectl rollout undo`.

## Viva prep: likely questions
- *Why poll SCM instead of a webhook?* GitHub can't reach a laptop's localhost Jenkins without a tunnel such as ngrok. Polling every minute needs no public URL.
- *What does the handler buy you?* Idempotency. The second run reports `changed=0` and doesn't restart the service.
- *Why does the bad rollout cause no downtime?* `maxUnavailable: 0` keeps old pods until a new pod is Ready, and the readiness probe never passes on the broken pod.
- *Why are hits inconsistent at 5 replicas?* Each pod has its own in-memory cache, so the app isn't stateless. The fix is a shared Redis.
- *Liveness vs readiness vs startup?* Restart if dead / stop sending traffic if not ready / don't judge liveness until boot finishes.
