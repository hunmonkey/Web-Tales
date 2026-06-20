# ShelfView repository instructions

Follow `AGENTS.md` and the master plan.

- Base all work on the actual Tales 1.24.4 code patterns.
- Implement one `SV-xxx` ticket per change set.
- Keep custom backend code and `/api/shelfview/v1` isolated.
- Keep custom frontend reader code isolated under a ShelfView module where practical.
- Preserve Vue 2, Vuex 3, Vuetify 2, TypeScript 4.9, JDK 21+, and the existing Gradle/Spring stack during MVP.
- Do not perform unrelated cleanup or dependency modernization.
- Reuse existing authentication, authorization, scanner, media analysis, page delivery, persistence, i18n, and error patterns.
- Add tests for behavior and authorization. Never claim a check passed unless it was run.
- Keep `/data` read-only and never expose raw server paths.
- Treat archives and TXT as hostile input.
- Do not copy GPL/AGPL source into the default implementation.
- Use versioned Manifest and Locator schemas.
- Use paragraph/character locators for TXT, not visual page numbers.
- Require `.shelfview-book` for image-directory books in MVP.
- Keep the old reader available behind fallback until migration is complete.
- Update `docs/AI_PROGRESS.md` after each task and create ADRs for irreversible decisions.
