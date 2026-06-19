# Upstream Sync Procedure

Verified during SV-001 on 2026-06-18.

## Current baseline

- Project remote: `origin` -> `https://github.com/hunmonkey/Web-Tales.git`
- Upstream remote: `upstream` -> `https://github.com/gotson/komga.git`
- Upstream push URL: `DISABLED`
- Base tag: Komga `1.24.4`
- Base tag commit: `9c84690a07aa7dcc5c54b58dd67de3fb8b9deb5d`
- Active development branch: `dev`
- Deployment branch: `main`

## Sync steps

1. Ensure the working tree is clean and create a backup tag.
2. Fetch `upstream` and the target release tag.
3. Read release, security, database migration and frontend dependency notes.
4. Create `upstream-sync/<target>` from `dev`.
5. Merge without history rewrite.
6. Resolve conflicts with priority on preserving upstream behavior; keep Tales hooks minimal.
7. Run baseline backend and frontend checks.
8. Run Tales API, reader, migration and authorization tests.
9. Build and smoke-test the Docker image against a copied config database.
10. Document migration and rollback, then merge back into `dev`.
11. After verification, fast-forward `main` to the tested `dev` commit and push both branches.

Never modify or renumber an existing upstream or Tales Flyway migration to resolve ordering. Add a new corrective migration when required.
