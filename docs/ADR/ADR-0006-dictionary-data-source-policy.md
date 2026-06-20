# ADR-0006: Dictionary data source and license policy

- Status: Accepted for planning
- Date: 2026-06-20
- Ticket: SV-930r (Phase 3 built-in dictionary data source policy)
- Supersedes: none
- Related: `docs/BACKLOG.md` SV-930, `docs/SECURITY_AND_LICENSE.md`

## Context

SV-930g added a safe dictionary handoff: selected text opens a web dictionary
search without copying third-party dictionary data into Tales.

The remaining "built-in dictionary data" idea needs a data-source decision
before implementation. Dictionary entries are content, not just code. Bundling
them changes the license and attribution work for the distributed app.

### Verified sources

Checked on 2026-06-20:

1. Public Data Portal, `문화체육관광부 국립국어원_개방형 사전(우리말샘)`
   - API data format is JSON/XML.
   - The page lists free use, real-time updates and traffic governed by the
     provider policy.
   - The license area lists third-party rights with attribution and share-alike,
     plus public-data attribution type 1.
   - Source: https://www.data.go.kr/data/15019347/openapi.do
2. National Institute of Korean Language Korean-English Learners' Dictionary
   - Describes the dictionary as the translated/bilingualized form of the Basic
     Korean Dictionary.
   - States that content can be used freely according to its copyright policy,
     but the current help page is not enough by itself to define a packaged
     redistribution plan.
   - Source: https://krdict.korean.go.kr/m/eng/help
3. Wiktionary
   - Entry text is dual-licensed under CC-BY-SA 4.0 and GFDL.
   - Users must follow attribution and share-alike/GFDL obligations.
   - Source: https://en.wiktionary.org/wiki/Wiktionary:Copyrights
4. kaikki.org machine-readable dictionaries
   - Extracted from Wiktionary and available under the same Wiktionary licenses:
     CC-BY-SA and GFDL.
   - Source: https://kaikki.org/dictionary/index.html

## Decision

Do **not** bundle third-party dictionary data in the default Tales/ShelfView
application until the owner explicitly accepts the selected data source and its
redistribution obligations.

Use a separate dictionary-pack model for future implementation:

1. The Tales code stays under the project code license.
2. Dictionary content lives in a separate data pack, cache or user-provided
   import, with its own metadata:
   - source name
   - source URL
   - source retrieval date
   - license name and URL
   - attribution text
   - redistribution notes
   - data schema version
3. A dictionary pack must not be mixed into the source tree without a matching
   notice entry and owner approval.
4. The first implementation should keep the existing web lookup handoff as the
   default and add one of these safe paths:
   - user-configured online API provider, where the user supplies the API key
     and accepts the provider terms;
   - owner-approved offline dictionary pack stored under `/config` or `/cache`;
   - a tiny first-party sample dictionary used only for tests and UI plumbing.
5. Do not ship Wiktionary/Kaikki-derived data as a default built-in pack until
   attribution, share-alike/GFDL handling and update scripts are reviewed.
6. Do not ship 우리말샘 or 한국어기초사전 data as an offline default pack until the
   exact API terms, attribution text, traffic/update policy and redistribution
   permissions are captured in notices.

## Consequences

Positive:

- Avoids accidentally changing the distribution obligations of the default app.
- Keeps dictionary features extensible without committing to one dataset too
  early.
- Lets self-hosted users choose an API key or data pack later.
- Keeps third-party attribution and data refresh logic auditable.

Negative:

- This ADR does not yet deliver offline dictionary definitions.
- A later implementation needs owner approval for the first real data source.
- Dictionary pack import/search will require schema, storage and UI tickets.

## Future implementation hook points

Small follow-up tickets should proceed in this order:

1. Add frontend dictionary provider state and UI that can show local results,
   configured online results or the existing web-search fallback.
2. Add a backend dictionary metadata endpoint only after a data-pack schema is
   chosen.
3. Add an owner-approved dictionary pack importer or API provider integration.
4. Add `THIRD_PARTY_NOTICES.md` entries for any bundled or cached redistributable
   dictionary content.

## Security and license notes

- Dictionary query text is user-selected content. Do not log full lookup
  queries by default.
- API keys for online dictionary providers must live in configuration, not in
  the source tree or client bundle.
- Dictionary HTML or markup from third-party sources must be sanitized or
  rendered as text. Do not use `v-html` for dictionary definitions without a
  dedicated sanitizer decision.
- Keep source media read-only; dictionary caches belong under `/config` or
  `/cache`.
