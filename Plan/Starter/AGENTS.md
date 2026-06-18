# ShelfView Agent Instructions

## Mission

Komga 1.24.4 포크를 기반으로 PDF, TXT, ZIP/CBZ, 명시적 이미지 폴더를 읽는 자체 호스팅 웹 리더를 만든다. 기존 Komga 호환성과 업스트림 병합 가능성을 보존한다.

## Read first

작업 전에 다음 파일을 읽는다.

1. `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`
2. `docs/ARCHITECTURE.md`
3. `docs/BACKLOG.md`
4. `docs/SECURITY_AND_LICENSE.md`
5. 관련 ADR
6. `docs/AI_PROGRESS.md`

## Baseline

- Base: Komga tag `1.24.4`
- Backend: Kotlin/Spring Boot/Gradle/jOOQ/Flyway
- Frontend: Vue 2.6/TypeScript 4.9/Vuex 3/Vuetify 2
- Requirements: JDK 21+, Node.js 18+
- New API prefix: `/api/shelfview/v1`
- New backend code: prefer `org.gotson.komga.shelfview`
- New frontend code: prefer `komga-webui/src/shelfview`
- Original media: read-only

The paths above are intentions, not permission to invent files. Audit the actual repository and follow existing patterns.

## Work protocol

- Work on exactly one `SV-xxx` ticket at a time.
- Before editing, locate existing analogous code and list planned files/tests.
- Prefer a small vertical slice over broad scaffolding.
- Do not refactor unrelated code.
- Keep existing public APIs backward compatible.
- Put new endpoints under `/api/shelfview/v1`.
- Use versioned Manifest and Locator schemas.
- Add or update tests with every behavioral change.
- Run the relevant checks and report exact commands and outcomes.
- Update `docs/AI_PROGRESS.md` before finishing.
- Write an ADR before a cross-cutting or hard-to-reverse decision.

## Forbidden without explicit owner approval

- Vue 3, Vite, Pinia, Spring, Kotlin, Gradle, or broad dependency migrations
- Editing an already-applied Flyway migration
- Force push, history rewrite, `git reset --hard`, mass rename
- Removing or weakening tests
- Bypassing authentication or authorization
- Writing into the source media library
- Copying GPL/AGPL code into the default implementation
- Adding a dependency without purpose and license review
- Returning raw filesystem paths to clients
- Rendering untrusted TXT through `v-html`

## Architecture rules

- Reuse Komga's scanner, media analysis, page streaming, auth, and progress patterns before inventing replacements.
- The UI consumes a unified ShelfView Manifest; it does not branch directly on extensions.
- Fixed-page formats use `FixedPageAdapter`.
- TXT uses `TextReflowAdapter` and paragraph/character locators.
- Bookmarks and last position are separate concepts.
- Image directories require `.shelfview-book` in MVP.
- PDF uses fixed page images in MVP; PDF.js is a later ADR-backed phase.
- Existing reader remains available as a fallback until the new reader is proven.

## Security rules

- Authenticate and authorize every custom content, position, bookmark, and preference endpoint.
- Validate all Locator and preference DTOs.
- Defend archives against traversal, absolute paths, symlinks, zip bombs, nested archives, excessive entries, and excessive image dimensions.
- Escape TXT and apply explicit size/line-length limits.
- Never log credentials, tokens, raw content, or full media paths.

## Completion report

End every task with:

1. Ticket and outcome
2. Changed files with reasons
3. Tests/checks actually run and results
4. Security, migration, API, and upstream compatibility notes
5. Remaining risks or follow-up ticket
6. `docs/AI_PROGRESS.md` update
