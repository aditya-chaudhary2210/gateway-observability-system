# Repository Folder Structure (Target Layout)

<!--
  Ownership: Platform Architecture
  Reasoning: Monorepo keeps cross-service contracts, k8s, and compose aligned for resume-grade coherence.
  Status: PHASE 1 — documented target; repositories may be sparse until phased implementation.
-->

This document defines the **intended production-style layout** for a single monorepo. Services are independently buildable Maven modules or npm workspaces; boundaries follow [SERVICE_BOUNDARIES.md](./architecture/SERVICE_BOUNDARIES.md).

## Root Layout

```
gateway-observability-system/
├── docs/                              # ← All Phase 1 documentation (this hub)
├── .github/
│   └── workflows/                     # CI/CD (lint, test, build, scan, compose smoke)
├── deploy/
│   ├── docker-compose/                # Local & demo stacks (infra + apps)
│   ├── k8s/                           # Namespaces, workloads, Helm or raw manifests
│   ├── prometheus/                    # Scrapes, alerts, recording rules
│   └── grafana/                       # Dashboards as code (JSON) + datasources
├── contracts/
│   ├── async/                         # Avro/JSON Schema/asyncapi (Phase 2+)
│   └── rest/                          # OpenAPI stubs per service façade
├── platform/
│   ├── observability-java/             # OTel starters, Micrometer conventions, correlation
│   └── testcontainers/               # Shared test scaffolding
├── services/
│   ├── api-gateway-service/           # Spring Cloud Gateway (+ security, resilience, simulation)
│   ├── log-ingestion-service/        # Kafka consumers, enrichment, persistence orchestration
│   ├── incident-detection-service/   # Rules + anomaly pipelines, clustering, scoring
│   └── notification-edge-service/    # Optional: webhook/email façade (later phase)
├── ai-analysis-service/              # Python FastAPI — RCA, similarity, prioritization APIs
├── dashboard-web/                     # React + TypeScript + Tailwind SPA
├── loadgen/                           # Optional k6 or Gatling scenarios for demos
├── LICENSE
├── README.md
└── pom.xml                            # Maven parent aggregator (Java services + platform BOM)
```

## Service Internals (Java — Clean Architecture Layers)

Applied consistently across Java services (`api-gateway-service`, `log-ingestion-service`, `incident-detection-service`):

```
services/<service>/
├── pom.xml
├── src/main/java/com/company/gatewayobs/<svc>/
│   ├── bootstrap/                     # Spring Boot application, configuration beans
│   ├── api/                           # REST/WebFlux adapters, DTO mapping, validation
│   ├── application/                 # Use cases, orchestrators, ports (interfaces)
│   ├── domain/                        # Entities, value objects, domain events (pure)
│   └── infrastructure/              # Kafka, DB, Redis, Feign clients, OTel exporters
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/                  # Flyway/Liquibase (if applicable)
└── src/test/
```

## AI Service Layout (Python)

```
ai-analysis-service/
├── app/
│   ├── main.py                         # FastAPI app factory
│   ├── api/routes/
│   ├── core/                           # Settings, logging, tracing
│   ├── domain/
│   ├── services/                       # LangChain pipelines, RCA orchestration
│   └── infrastructure/               # Redis/Postgres/ES/OpenSearch clients
├── tests/
├── Dockerfile
├── pyproject.toml OR requirements.txt
└── README.md
```

## Frontend Layout

```
dashboard-web/
├── public/
├── src/
│   ├── app/                           # Routing shell
│   ├── features/                    # Incident feed, traces, RCA panels
│   ├── components/
│   ├── hooks/
│   ├── services/                    # API clients (typed)
│   └── styles/
├── package.json
├── tailwind.config.ts
├── vite.config.ts                     # Or CRA — decision in DECISION_LOG
└── Dockerfile
```

## Naming & Module Rules

| Rule | Rationale |
|------|-----------|
| **`services/*` owns runtime deployables** | Clear Ops boundary vs `platform/*` libs |
| **`contracts/*` is the single handshake** | Avoids implicit DTO duplication |
| **`deploy/*` is environment code** | GitOps-adjacent; reviewable infra |
| **No shared “utils” dumping ground** | Use `platform/observability-java` with explicit APIs |

## Cross-References

- Deployment packaging: [infra/DOCKER_STRATEGY.md](./infra/DOCKER_STRATEGY.md), [infra/K8S_STRATEGY.md](./infra/K8S_STRATEGY.md)
- Event payloads: [streaming/KAFKA_TOPICS.md](./streaming/KAFKA_TOPICS.md)
- Tracking implementation against this layout: [tracking/IMPLEMENTATION_PROGRESS.md](./tracking/IMPLEMENTATION_PROGRESS.md)
