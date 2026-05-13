# Coding Guidelines

<!--
  Ownership: Engineering Guild
-->

## 1. Why Discipline Wins Resume Reviews

Consistency signals **staff-level breadth**: onboarding speed, predictable reviews, measurable quality.

---

## 2. Java (Spring Boot 3 / Java 21)

| Topic | Rule |
|-------|------|
| **Architecture** | Strict layer boundaries per [LLD.md](../architecture/LLD.md): `domain` has zero framework imports. |
| **DTOs** | Immutable records for external payloads; Bean Validation on inbound only. |
| **Null safety** | Use `Optional` thoughtfully; forbid returning null collections. |
| **Concurrency** | No blocking calls on reactive event loop threads; offload with bounded schedulers if unavoidable. |
| **Dependencies** | Constructor injection exclusively. |
| **Logging** | SLF4J + JSON structured layout (`logstash-logback-encoder` or equivalent). Include `trace_id`, `correlation_id`. |
| **Configuration** | `@ConfigurationProperties` strongly typed beans; secrets via env—not hard-coded. |

---

## 3. Python (FastAPI)

| Topic | Rule |
|-------|------|
| **Typing** | `mypy --strict` aspiration; min `pytyped` stubs for domain modules |
| **Async** | `async def` endpoints; offload CPU parsing if needed (`run_in_executor`) |
| **Pydantic models** | Versioned schemas for LLM structured outputs validation |
| **Dependency injection** | FastAPI Depends pattern wrappers for gateways |

---

## 4. TypeScript / React

| Topic | Rule |
|-------|------|
| **Strict TS** | `strict: true`; no implicit `any` |
| **State** | Server state vs UI state separation (TanStack Query recommended — decide in DECISION_LOG) |
| **Styling** | Tailwind utilities; isolate feature-level components |

---

## 5. Testing Expectations

| Layer | Requirement |
|-------|--------------|
| Unit | Pure domain logic >80% meaningful branches |
| Integration | Testcontainers subset for kafka/pg/redis/es pivotal paths |
| Contract | Consumer-driven async schema fixtures |

Coverage target aspirational—not vanity %; prioritize critical pipelines.

---

## 6. Performance Hygiene

- Avoid N+1 repository patterns; batch ingestion uses bulk APIs.
- Pre-size collections when bounds known.

---

## 7. Forbidden Patterns

| Anti-pattern | Replace with |
|--------------|---------------|
| God service classes | Dedicated use-case classes (<300 LOC heuristic) |
| Shared mutable singleton state | Beans + caches with TTL |
| Catch-all swallowed exceptions | Central handler + categorized domain exceptions |

---

## Related

- [ERROR_HANDLING_GUIDELINES.md](./ERROR_HANDLING_GUIDELINES.md)
- [PR_REVIEW_GUIDELINES.md](./PR_REVIEW_GUIDELINES.md)
