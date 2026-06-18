---
name: review-and-test
description: Review a ShelfView ticket for correctness, security and regressions
argument-hint: "SV-xxx or changed file range"
agent: agent
---

Review `${input:ticket:current changes}` as an independent senior maintainer.

Read the project instructions and inspect the diff. Do not start with style comments.
Prioritize:

1. Authentication and authorization gaps.
2. Raw path exposure, traversal, archive bomb and unsafe text rendering.
3. Data migration and Locator compatibility.
4. Existing Komga API/reader/progress regressions.
5. Resource leaks, stale requests, memory and large-file behavior.
6. 0/1-based page errors and natural sort errors.
7. Missing tests and false claims in documentation.
8. Unnecessary dependencies or license conflicts.
9. Upstream merge conflict surface.

Fix defects that are within the ticket scope. For broader issues, add a backlog item.
Run the narrow tests first, then the relevant full checks.

Return findings ordered by severity, changed files, commands run, outcomes, and any residual risk. Update `docs/AI_PROGRESS.md`.
