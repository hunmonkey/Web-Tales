# Upstream Sync Procedure

This document is a starting template. SV-001 must verify and update it against the actual repository.

1. Ensure working tree is clean and create a backup tag.
2. Fetch `upstream` and the target release tag.
3. Read release, security, database migration and frontend dependency notes.
4. Create `upstream-sync/<target>` from `shelfview-main`.
5. Merge without history rewrite.
6. Resolve conflicts with priority on preserving upstream behavior; keep ShelfView hooks minimal.
7. Run baseline backend and frontend checks.
8. Run ShelfView API, reader, migration and authorization tests.
9. Build and smoke-test the Docker image against a copied config database.
10. Document migration and rollback, then merge by PR.

Never modify or renumber an existing upstream or ShelfView Flyway migration to resolve ordering. Add a new corrective migration when required.
