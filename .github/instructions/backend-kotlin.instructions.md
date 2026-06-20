---
name: ShelfView Backend Kotlin
description: Kotlin, Spring, jOOQ, Flyway, API and persistence rules
applyTo: "tales/**/*.kt,tales/**/*.kts,tales/**/db/migration/**/*"
---

# Backend rules

- Find and imitate the nearest Tales controller, service, repository, event, DTO, and test pattern.
- Prefer `org.tales.shelfview` for new code unless an existing extension point requires a minimal hook elsewhere.
- Put new endpoints under `/api/shelfview/v1` and reuse existing auth/authorization helpers.
- Keep DTO, domain, and persistence models separate.
- Validate enum values, page bounds, Locator schema, encoding, scope, and payload size.
- Never edit an existing Flyway migration. Inspect the latest migration and create a new timestamped migration following repository convention.
- Add repository/API integration tests, including unauthorized and forbidden cases.
- Do not reveal raw filesystem paths in DTOs, logs, or errors.
- Cancellation, streaming, resource closure, and memory limits are mandatory for large content.
