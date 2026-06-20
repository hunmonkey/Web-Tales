---
name: upstream-sync
description: Plan and validate a Tales upstream merge
argument-hint: "target Tales tag or branch"
agent: plan
---

Plan an upstream synchronization to `${input:target:target tag}`.

Do not rewrite history. Inspect remotes, current base, release notes, security notes, migrations, frontend dependency changes and reader changes.

Produce:

1. Proposed sync branch and commands.
2. Expected conflicts, especially ShelfView hooks.
3. Database migration ordering risks.
4. Required baseline, merge and post-merge tests.
5. Rollback plan.
6. Documentation/version changes.

Do not merge until the plan is reviewed. Record the plan in `docs/AI_PROGRESS.md`.
