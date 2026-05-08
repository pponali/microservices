# CI/CD pipeline

`.github/workflows/cd.yml` builds and deploys to the OCI k3s VM (`apps.khetisahayak.com`, `80.225.224.96`) on every push to `master`.

```
push to master
   │
   ├── detect-changes   diffs HEAD~1..HEAD; emits the list of <module>/ dirs that changed
   │                    (or all modules, if pom.xml / helm/ / cd.yml changed)
   │
   ├── build-push       matrix job on ubuntu-24.04-arm (native arm64, no QEMU);
   │                    docker build + push pponali/<svc>:latest and pponali/<svc>:<sha>
   │
   └── deploy           SSH to VM → scp helm/ → helm upgrade → rollout-restart only
                        the changed Deployments → wait for rollouts
```

## One-time setup

### 1. GitHub repository secrets

Settings → Secrets and variables → Actions → **New repository secret**:

| Name | Value |
| --- | --- |
| `DOCKERHUB_USERNAME` | `pponali` |
| `DOCKERHUB_TOKEN` | Docker Hub access token (Account Settings → Security → New Access Token, "Read, Write, Delete" scope) |
| `SSH_HOST` | `80.225.224.96` |
| `SSH_USER` | `ubuntu` |
| `SSH_KEY` | The full private key in PEM format (`-----BEGIN OPENSSH PRIVATE KEY-----` … `-----END OPENSSH PRIVATE KEY-----`). Generate a dedicated CI key: `ssh-keygen -t ed25519 -f ~/.ssh/oci_ci -N ""` then `ssh-copy-id -i ~/.ssh/oci_ci ubuntu@80.225.224.96`. Paste `cat ~/.ssh/oci_ci` into the secret. |

### 2. VM prerequisites (one-time)

The pipeline expects the VM to have k3s + kubectl + docker (already installed). It self-installs Helm and cert-manager on the first deploy.

OCI security list — open inbound TCP 80 and 443 for Let's Encrypt + the ingress:

```
# In the OCI console: VCN → Security Lists → Default Security List → Add Ingress Rules
# Source CIDR 0.0.0.0/0 → TCP destination port 80
# Source CIDR 0.0.0.0/0 → TCP destination port 443
```

Also on the VM:

```
sudo iptables -I INPUT -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save   # Ubuntu 24.04
```

### 3. DNS

`apps.khetisahayak.com` A → `80.225.224.96` (already done — verified via `dig`).

## How a deploy unfolds

1. `git push` to `master`.
2. `detect-changes` job lists changed modules. If only one service changed, only that one rebuilds.
3. `build-push` builds the new image natively on ARM and pushes both `:latest` and `:<sha>` tags.
4. `deploy` job:
   - syncs the latest chart to `~/microservices/` on the VM,
   - runs `helm upgrade` (re-renders templates against current `values.yaml`),
   - runs `kubectl rollout restart` on each changed Deployment so k3s pulls the new `:latest`.

## Force a full rebuild

GitHub UI → Actions → "CD - build & deploy to OCI k3s" → Run workflow → check **force_all**.

## Local fallback

`scripts/build-and-push.sh` (run on the VM) and `scripts/deploy.sh` are kept as manual escape hatches; CI is the canonical path.

## Troubleshooting

- **TLS cert pending** — `kubectl -n microservices describe certificate` and `kubectl -n cert-manager logs deploy/cert-manager`. Common cause: port 80 not reachable from the public internet (OCI security list).
- **Pods stuck in `ImagePullBackOff`** — check `kubectl -n microservices describe pod <pod>`. Either the image didn't push (check Docker Hub) or `imagePullPolicy: Always` is racing the registry; restart again.
- **Build job slow** — first build per service is full Maven download; subsequent builds use `cache-from: type=gha` and are 5-10× faster.
- **Eureka registration loops** — set `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/` for the affected service in `helm/microservices/values.yaml`.
