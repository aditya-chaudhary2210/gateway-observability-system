# Pull Request Review Guidelines

<!--
  Ownership: Engineering
-->

## 1. Review Goals

Improve **correctness**, **security**, **maintainability**, **operability**.

---

## 2. Mandatory Author Checklist

- [ ] Documentation updated (`docs/` or README if behaviour surfaces externally)
- [ ] Tests augment changed paths
- [ ] Metrics/logs for new failure modes
- [ ] No secrets
- [ ] Performance considerations for hot paths articulated (even if measured later)
- [ ] Feature flags default safe non-prod

---

## 3. Review Dimensions

| Lens | Questions |
|------|-----------|
| Architecture | Layers respected? Unexpected coupling introduced? |
| Data | Migration backward compatible or dual-write staged? Idempotency ensured? |
| Resilience | Timeouts retries circuit break sensible? Infinite retry absent? |
| Security | Injection paths? JWT validation unaffected? Sensitive logging absent? |
| Observability | New counters/histograms? Trace propagation preserved? |

---

## 4. Blocking vs Non-Blocking

**Blocking:**

- correctness bugs
- security flaws
- data loss regressions missing recovery path

**Non-blocking (issue ticket):**

- cosmetic refactors delaying merge
- nice-to-have micro-optimisations

Annotate GH inline comments severity.

---

## 5. LLM Generated Code Clause

Allowed if reviewer can explain every line logically; disallow blind paste without tests.

---

## Related

- [CODING_GUIDELINES.md](./CODING_GUIDELINES.md)
- [ERROR_HANDLING_GUIDELINES.md](./ERROR_HANDLING_GUIDELINES.md)
