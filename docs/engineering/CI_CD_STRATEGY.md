# CI/CD Strategy

<!--
  Ownership: Platform / DevOps
-->

## 1. Goals

| Goal | KPI |
|------|-----|
| Fast feedback | <10 min MVP pipeline |
| Security shift-left | Blocking high CVE on direct path dependencies |
| Reproducible builds | Locked base images checksums Phase 8 |
| Traceability | Image tag = git SHA |

---

## 2. Pipeline Stages (GitHub Actions baseline)

_Planned workflows under `.github/workflows/`._

| Stage | Actions | Blocking? |
|-------|---------|-----------|
| **Preflight** | checkout, semver sanity | yes |
| **Java** | `./mvnw -B verify` aggregator | yes |
| **Python AI** | `ruff fmt check`, `pytest`, optional `mypy` | yes |
| **Frontend** | `pnpm lint`, `pnpm test`, `pnpm build` | yes |
| **Container** | `docker build` multi-service matrix | yes (smoke) |
| **Compose Smoke** | `docker compose config` + up subset health wait | advisory early → blocking hardened |
| **Security** | Trivy image scan dependency review | CVE threshold policy |
| **Publish** _(optional)_ | GHCR tagged images SHA | gated main branch |

Parallelize independent jobs (matrix: module).

---

## 3. Artifact Strategy

| Artifact | Destination |
|----------|-------------|
| JAR layered image | GHCR `/gateway-obs-api` etc. |
| Python wheel | Embed in AI image ephemeral |
| Static SPA | Nginx stage artifact digest |

SBOM (`syft`) generation stretch Phase 8.

---

## 4. Promotion Model

Branches:

- `feature/*` → CI only  
- merge `main` → publish candidate images tagging `sha-<SHORT>`  

Deployment automation **not** prerequisite for MVP resume storyline; manifests applied manually or scripted.

---

## 5. Secrets in CI

Use GitHub Encrypted Secrets for publishing tokens; ephemeral fine-grained PAT minimal scope.

_No long-lived kubeconfig in novice portfolio unless rotation story documented._

---

## 6. Quality Gates Escalation

If flake emerges:

1. Quarantine flaky test tagging `@Disabled` ONLY with tracking issue—not silent removal.
2. Add retry **test harness** sparingly—not production masking.

---

## 7. Rollback

Image promotion rollback: redeploy prior digest; DB migrations reversible or expand-contract patterns mandatory for risky DDL.

---

## Related

- [BRANCHING_STRATEGY.md](./BRANCHING_STRATEGY.md)
- [../infra/K8S_STRATEGY.md](../infra/K8S_STRATEGY.md)
