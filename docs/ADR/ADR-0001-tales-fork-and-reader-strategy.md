# ADR-0001: Tales fork and incremental unified reader

- Status: Accepted for planning
- Date: 2026-06-18

## Context

ShelfView needs library scanning, users, permissions, metadata, page delivery, progress, web UI and Docker deployment. Rebuilding all foundations would delay the reader features and increase security risk.

## Decision

Fork Tales from stable tag 1.24.4. Preserve its existing backend and frontend architecture during MVP. Add an isolated ShelfView API namespace and reader module. Introduce the new reader behind a feature flag and retain the old reader as fallback.

Fixed-page formats reuse Tales's existing page delivery path. TXT uses a new reflow adapter and stable text Locator. Image-directory books require an explicit `.shelfview-book` marker. PDF.js is deferred until text-layer features are explicitly scheduled.

## Consequences

Positive:

- Fast path to a usable server
- Existing auth, scanning and page delivery are reused
- Lower initial security and operational risk
- Upstream fixes can be merged

Negative:

- Vue 2 and existing architectural constraints remain during MVP
- Some extension hooks may still require small upstream-adjacent changes
- Care is required to keep DB migrations and progress compatible

## Rejected alternatives

- Build every service from scratch: too much foundation work.
- Fork Koodo as server foundation: AGPL and browser-centric storage differ from the desired server model.
- Replace the whole reader immediately: rollback and regression risk is too high.
- Introduce PDF.js in phase 1: unnecessary complexity before the common fixed-page reader works.

## Revisit triggers

- Tales architecture prevents TXT or image-directory integration without broad invasive changes.
- Vue 2 blocks required accessibility or browser support.
- Direct PDF text/search becomes a release requirement.
