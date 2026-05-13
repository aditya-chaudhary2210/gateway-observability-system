# Branching Strategy

<!--
  Ownership: Engineering
-->

## 1. Trunk-Based Variant (Portfolio Scale)

Why: reduces long-lived divergence while SDE-2 resume scope stays bounded.

Baseline:

| Branch | Role |
|--------|------|
| `main` | Always deployable-ish; protected |
| `feature/<ticket>-slug` | Short-lived features |
| `fix/<ticket>-slug` | Bugs / reliability |
| `release/x.y.z` | Optional stabilization tag phases only |

Avoid month-long forks.

---

## 2. Commit Conventions

`type(scope): summary`

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`.

Example: `feat(gateway): add correlation-id propagation filter`

Scopes mirror service/module names (`gateway`, `ingestion`, `detection`, `ai`, `dashboard`, `infra`).

---

## 3. Merge Policy

Pull requests REQUIRED for `main` with ≥1 reviewer (SELF review allowed only if solo project—still use checklist mentally).

Squash merges preferred preserving clean linear history narrative.

---

## 4. Hotfix Lane

Cherry-pick serious prod defect fix from tagged release if branching model evolves—portfolio default simple path: direct fix PR to `main` + tag micro version.

---

## 5. Feature Flags Alignment

Incomplete capabilities shipped dark via configuration toggles aligning with phased roadmap.

---

## Related

- [PR_REVIEW_GUIDELINES.md](./PR_REVIEW_GUIDELINES.md)
