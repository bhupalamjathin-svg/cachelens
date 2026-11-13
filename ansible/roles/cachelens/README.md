# cachelens role

Scaffolded with `ansible-galaxy init roles/cachelens`.

Installs OpenJDK 17, copies the CacheLens JAR built by Maven/Jenkins, writes an environment file
(port, cache policy, capacity) and a systemd unit, then starts the service and health-checks it.

| Variable | Default | Purpose |
|---|---|---|
| `app_port` | 8080 | Port the API listens on |
| `cache_policy` | LRU | `LRU` or `LFU` |
| `cache_capacity` | 5 | Max keys before eviction |
| `artifact_src` | `../target/cachelens-*.jar` | JAR to deploy |

Handler `Restart cachelens` fires only when the JAR, env file or unit file changed.
