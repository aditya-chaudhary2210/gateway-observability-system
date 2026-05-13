# AI-Powered API Gateway Observability Platform — Documentation Hub

<!--
  Ownership: Platform / Architecture Guild
  Purpose: Single entry point for engineers joining the project or resuming multi-session work.
  Update cadence: When new bounded contexts, topics, or runbooks are added.
-->

This directory contains **Phase 1** artefacts: architecture, contracts-at-doc-level, operations strategy, and **persistent context** for implementation (progress, decisions, debt).

## How to Read These Docs

1. Start with [Repository Folder Structure](./REPOSITORY_FOLDER_STRUCTURE.md) and [architecture/HLD.md](./architecture/HLD.md).
2. Use [architecture/SERVICE_BOUNDARIES.md](./architecture/SERVICE_BOUNDARIES.md) for ownership and coupling rules.
3. Before changing data plane behaviour, align [streaming/KAFKA_TOPICS.md](./streaming/KAFKA_TOPICS.md), [streaming/RETRY_STRATEGY.md](./streaming/RETRY_STRATEGY.md), and [streaming/DLQ_STRATEGY.md](./streaming/DLQ_STRATEGY.md).
4. Before changing persistence, align [database/POSTGRES_SCHEMA.md](./database/POSTGRES_SCHEMA.md), [database/REDIS_USAGE.md](./database/REDIS_USAGE.md), and [database/ELASTICSEARCH_INDEXES.md](./database/ELASTICSEARCH_INDEXES.md).
5. For AI behaviour (non-gimmicky), read [ai/AI_PIPELINE.md](./ai/AI_PIPELINE.md) and [ai/PROMPT_STRATEGY.md](./ai/PROMPT_STRATEGY.md).
6. For day-to-day engineering norms: [engineering/CODING_GUIDELINES.md](./engineering/CODING_GUIDELINES.md) and adjacent files.
7. **Always** update [tracking/IMPLEMENTATION_PROGRESS.md](./tracking/IMPLEMENTATION_PROGRESS.md) and [tracking/DECISION_LOG.md](./tracking/DECISION_LOG.md) when scope or design changes.

## Index

| Area | Documents |
|------|-----------|
| **Overview** | [Repository folder structure](./REPOSITORY_FOLDER_STRUCTURE.md) |
| **Architecture** | [HLD](./architecture/HLD.md), [LLD](./architecture/LLD.md), [Service Boundaries](./architecture/SERVICE_BOUNDARIES.md), [Event-Driven](./architecture/EVENT_DRIVEN_ARCHITECTURE.md), [Sequences](./architecture/SEQUENCE_DIAGRAMS.md), [Failure Handling](./architecture/FAILURE_HANDLING.md), [Future scaling](./architecture/FUTURE_SCALING_STRATEGY.md) |
| **Contracts** | [REST / API overview](./contracts/API_CONTRACTS.md) _(OpenAPI artefacts under `/contracts/rest` Phase 2+)_ |
| **AI** | [AI Pipeline](./ai/AI_PIPELINE.md), [Model strategy](./ai/AI_MODEL_STRATEGY.md), [Prompt Strategy](./ai/PROMPT_STRATEGY.md), [RCA Flow](./ai/RCA_GENERATION_FLOW.md), [Incident Classification](./ai/INCIDENT_CLASSIFICATION.md) |
| **Infrastructure** | [Deployment](./infra/DEPLOYMENT_ARCHITECTURE.md), [Docker](./infra/DOCKER_STRATEGY.md), [Kubernetes](./infra/K8S_STRATEGY.md), [Observability stack](./infra/OBSERVABILITY_STACK.md), [Security](./infra/SECURITY_STRATEGY.md) |
| **Data Stores** | [PostgreSQL](./database/POSTGRES_SCHEMA.md), [Redis](./database/REDIS_USAGE.md), [Elasticsearch](./database/ELASTICSEARCH_INDEXES.md) |
| **Streaming** | [Kafka Topics](./streaming/KAFKA_TOPICS.md), [DLQ](./streaming/DLQ_STRATEGY.md), [Retry](./streaming/RETRY_STRATEGY.md) |
| **Engineering** | [Coding](./engineering/CODING_GUIDELINES.md), [Branching](./engineering/BRANCHING_STRATEGY.md), [PR Review](./engineering/PR_REVIEW_GUIDELINES.md), [Errors](./engineering/ERROR_HANDLING_GUIDELINES.md), [Local development](./engineering/LOCAL_DEVELOPMENT.md), [CI/CD](./engineering/CI_CD_STRATEGY.md) |
| **Tracking** | [Progress](./tracking/IMPLEMENTATION_PROGRESS.md), [Tech Debt](./tracking/TECH_DEBT.md), [Known Issues](./tracking/KNOWN_ISSUES.md), [Decision Log](./tracking/DECISION_LOG.md) |

## Document Conventions

- **Ownership** lines at the top of each file denote the owning team/context (gateway, ingestion, detection, ai, frontend, platform).
- **ADR-lite** entries live in [tracking/DECISION_LOG.md](./tracking/DECISION_LOG.md); deep dives stay in architecture/ai/infra docs.
- **Schemas** described here are **contract-first**; OpenAPI/async schema artifacts will mirror these docs in Phase 2+.
