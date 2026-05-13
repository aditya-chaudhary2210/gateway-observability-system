# Security Strategy

<!--
  Ownership: Security Architecture (logical) / Platform
-->

## 1. Threat Model Snapshot (Portfolio Realism)

| Threat | Mitigation pillar |
|--------|-------------------|
| Token theft / forged JWT | Validate signature issuer audience clock skew JWKS caching |
| Traffic flooding | Gateway rate limiting RL metrics IP-based heuristic optional later |
| Lateral movement | Namespace network policies minimal egress from AI pods |
| Data exfiltration via logs | Redaction/tokenization configurable field blocklist |
| Dependency CVEs | CI dependency scanning Maven pip npm audit periodic base image bumps |
| Insider misconfig demo flags | Fail simulation **disabled unless explicit profile `chaos`** |

Formal STRIDE worksheet optional stretch goal.

---

## 2. Edge Security (Gateway)

| Control | Detail |
|---------|--------|
| TLS | Terminate ingress TLS; mutual TLS mesh future |
| JWT | OAuth2 Resource Server; reject missing `Bearer` routes marked `secured` |
| Headers | Strip dangerous hop-by-hop; add security headers outbound optional |
| CORS | Dashboard origin allowlist—not `*` in non-dev |

---

## 3. Secret Management

| Environment | Approach |
|-------------|----------|
| Local | `.env` gitignored from `.env.template` |
| K8s | Secrets mounted env; eventual ExternalSecrets sealed secrets |
| Rotation | Manual early; automate calendar later |

Forbidden: committing API keys—even “demo” keys that bypass spend caps.

---

## 4. Data Classification

Default class **internal engineering telemetry**:

- Minimal user identity (**hash** `client_id`).
- Payload bodies **never** raw by default; digest only.

Elevated classifications require classification doc update + encryption-at-rest policy extension.

---

## 5. AI-Specific Risks

| Risk | Control |
|------|---------|
| Prompt injection via field values | Structural JSON evidence pack + delimiter markers + output schema validation |
| Model memorization leakage | No customer secrets in prompts |
| SSRF via tool misuse | Explicit allow-listed internal HTTP clients Phase-gated |

---

## 6. Compliance Posture Statement

Portfolio project—**no** asserted SOC2 HIPAA alignment; design hints prepare future hardening checklist.

---

## 7. Security Review Hooks

PR checklist augmentation:

- JWT filter changes ⇒ security reviewer or self-QA scripted attack cases
- Kafka schema adds ⇒ PII classifier review flag

---

## Related

- [DEPLOYMENT_ARCHITECTURE.md](./DEPLOYMENT_ARCHITECTURE.md)
- [DOCKER_STRATEGY.md](./DOCKER_STRATEGY.md)
- [../engineering/CODING_GUIDELINES.md](../engineering/CODING_GUIDELINES.md)
