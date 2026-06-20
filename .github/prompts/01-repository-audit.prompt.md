---
name: repository-audit
description: Audit the Tales fork before ShelfView implementation
argument-hint: "optional focus area"
agent: plan
---

Read [AGENTS.md](../../AGENTS.md), the
[master plan](../../SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md), and
[architecture](../../docs/ARCHITECTURE.md).

Perform ticket **SV-000**. Do not implement features and do not change production code.

Inspect the actual repository and record:

1. Current Git branch, tag/base commit, remotes, and dirty files.
2. JDK, Node, Gradle, frontend package versions and exact build/test commands.
3. Actual paths and main types for:
   - library scanning and supported extensions
   - media analysis and page enumeration
   - page image/content API
   - authentication and per-library/book authorization
   - reading progress and read status
   - database repositories, jOOQ and Flyway migrations
   - existing web reader route, view, state, services and tests
   - Docker/JReleaser build
4. Two or more existing patterns that ShelfView should reuse for each layer.
5. Likely minimal hook points and likely upstream merge conflicts.
6. Any fact in the master plan that is inaccurate for this checkout.
7. Baseline commands you actually ran and their exact results.

Write the result under a dated **SV-000** section in
[docs/AI_PROGRESS.md](../../docs/AI_PROGRESS.md).

Return a concise code map with real file paths. Do not invent APIs or paths.
