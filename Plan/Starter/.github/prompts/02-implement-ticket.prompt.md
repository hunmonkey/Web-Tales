---
name: implement-ticket
description: Implement one ShelfView backlog ticket safely
argument-hint: "SV-xxx and optional constraints"
agent: agent
---

Implement exactly this ticket: `${input:ticket:SV-xxx}`.

First read:

- [AGENTS.md](../../AGENTS.md)
- [master plan](../../SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md)
- [backlog](../../docs/BACKLOG.md)
- [security and license rules](../../docs/SECURITY_AND_LICENSE.md)
- [AI progress](../../docs/AI_PROGRESS.md)
- relevant ADRs

Before editing, state:

1. Ticket goal and explicit non-goals.
2. Existing code patterns and exact files to reuse.
3. Planned files to modify/create.
4. Tests and commands to run.
5. Security, database, API and upstream-merge risks.

Then implement the smallest complete vertical slice. Do not perform unrelated refactors or framework upgrades. Keep the old reader working.

At the end:

- run the relevant checks,
- report exact outcomes,
- update `docs/AI_PROGRESS.md`,
- propose a follow-up ticket instead of silently expanding scope.
