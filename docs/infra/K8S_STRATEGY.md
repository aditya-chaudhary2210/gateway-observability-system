# Kubernetes Strategy

<!--
  Ownership: Platform
-->

## 1. Cluster Layout

Namespace: `gateway-observability`

Sub-namespaces optional later (`go-ingress`, `go-data`, `go-apps`).

---

## 2. Workload Patterns

| Workload | Kind | Notes |
|----------|------|-------|
| Gateway | Deployment + HPA | PDB minAvailable 1 |
| Ingestion | Deployment (consumer) | scale with partition count |
| Detection | Deployment | CPU bound HPA |
| AI | Deployment | memory heavy; consider KEDA **future** off lag |
| Dashboard | Deployment or static S3 | MVP: nginx container |

---

## 3. ConfigMaps & Secrets

- `ConfigMap` per service for non-secret toggles.
- `Secret` for DB URLs, Kafka SASL, LLM API keys.

---

## 4. Probes

| Probe | Path | Purpose |
|-------|------|---------|
| Liveness | `/actuator/health/liveness` | Pod restart |
| Readiness | `/actuator/health/readiness` | Traffic membership |

AI: `/health` includes dependency check **degraded** mode if LLM unreachable but service still serving cached reads (policy choice).

---

## 5. Ingress

`Ingress` or `Gateway API` with TLS termination; rate limit at edge still complemented by app-level RL.

---

## 6. Observability

PodMonitor/ServiceMonitor CRs if Prometheus operator present; else annotation scrape legacy.

---

## 7. Progressive Delivery

Blue/green optional — out of scope MVP; feature flags via config reload.

---

## 8. Manifests Organization

```
deploy/k8s/
├── base/
│   ├── kustomization.yaml
│   ├── gateway-deployment.yaml
│   └── ...
└── overlays/
    ├── local/
    └── prod/
```

---

## Related

- [DEPLOYMENT_ARCHITECTURE.md](./DEPLOYMENT_ARCHITECTURE.md)
- [OBSERVABILITY_STACK.md](./OBSERVABILITY_STACK.md)
