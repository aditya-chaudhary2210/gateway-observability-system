# Deployment Architecture

<!--
  Ownership: Platform / DevOps
-->

## 1. Environments

| Env | Purpose | Notes |
|-----|---------|-------|
| `local-compose` | Developer laptop | Fast feedback; tolerates single broker ES node |
| `demo` | Resume portfolio | Single cloud VM or Codespaces possible |
| `staging` | Parity rehearsal | Mirrors prod topology smaller |
| `prod` *(aspirational)* | Realistic hardening showcase | HA Kafka, HA ES baseline |

---

## 2. Topology

**Edge:** Ingress → `api-gateway-service` Deployment (HPA CPU + custom metric `kafka_producer_lag` optional later).

**Data plane:** Kafka Strimzi / managed Kafka placeholder; Postgres managed; Redis managed; ES/OS managed or self-operated for demo.

**Observability plane:** Prometheus + Grafana; OTel Collector DaemonSet/agent pattern.

---

## 3. Network Zones

```
Internet -> Ingress -> Gateway -> Internal mesh (Kafka, DB, Redis, ES restricted SGs)
Dashboard static assets CDN optional
AI service reachable only internally + controlled egress LLM endpoint
```

No direct DB exposure from frontend.

---

## 4. GitOps Orientation

Desired end-state: manifests in `deploy/k8s` applied via ArgoCD/Flux (document only now); CI verifies `kubectl dry-run`/helm lint later.

---

## 5. Secrets & Config

Secrets: Kubernetes Secrets mounting as env vars; `.env.template` locally (never real secrets committed).

Rotate: manual initial; integrate ExternalSecrets later (TECH_DEBT).

---

## 6. Disaster Recovery Snapshot

Backup strategies:

| Store | Approach |
|-------|----------|
| Postgres | PITR for prod aspiration; nightly snapshot demo |
| Kafka | Topic retention sizing for replay-only recovery |
| ES | Snapshot repository S3-compatible (future) |

---

## 7. Related

- [DOCKER_STRATEGY.md](./DOCKER_STRATEGY.md)
- [K8S_STRATEGY.md](./K8S_STRATEGY.md)
- [OBSERVABILITY_STACK.md](./OBSERVABILITY_STACK.md)
