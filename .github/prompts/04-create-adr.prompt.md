---
name: create-adr
description: Create an Architecture Decision Record before a hard-to-reverse change
argument-hint: "decision topic"
agent: plan
---

Create a new ADR for `${input:topic:decision topic}` under `docs/ADR/`.

Inspect existing code and prior ADRs. Include:

- Status and date
- Context and problem
- Constraints
- At least two viable options
- Security, performance, license, migration and upstream-sync impact
- Decision and rationale
- Consequences and rejected options
- Rollback/revisit trigger
- Concrete implementation boundaries

Do not implement the decision in this task. Add affected ticket references to `docs/BACKLOG.md` and record the ADR in `docs/AI_PROGRESS.md`.
