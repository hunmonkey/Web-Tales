# ShelfView Security and License Guardrails

## Security boundary

Media files, archives, text, images, metadata, filenames, bookmark notes and settings are untrusted. A logged-in user is not automatically authorized for every library or book.

## Mandatory controls

- Authenticate and authorize every ShelfView endpoint.
- Do not expose raw server paths.
- Mount source media read-only.
- Keep derived output in config/cache.
- Validate versioned Locator and Manifest data.
- Enforce archive entry, size, ratio, nesting, path and pixel limits.
- Escape TXT and do not render untrusted HTML.
- Close streams, cancel stale work and bound memory.
- Avoid secrets in repository, logs and Docker layers.

## License default

- Preserve Tales MIT notices.
- Prefer MIT, BSD or Apache-2.0 dependencies with recorded purpose and license.
- Do not copy GPL/AGPL code into the default codebase.
- Koodo/Kookit, Kavita and BookLore are reference-only unless the owner intentionally changes the distribution obligations.
- Record any copied source file, origin URL, commit, license and modifications in `THIRD_PARTY_NOTICES.md`.
- Treat bundled dictionary data as third-party content unless it is fully first-party; record source, license, attribution and update policy before packaging it.
- AI-generated code that closely mirrors a third-party implementation must be rewritten or attributed after review.

## Dependency review checklist

Before adding a package:

1. Is it necessary, or can existing Tales code solve the problem?
2. Is it maintained and compatible with the current Vue 2/JDK stack?
3. What is its license?
4. Does it add native binaries, workers or network behavior?
5. What is its browser/server bundle cost?
6. Is it pinned and covered by tests?
7. Is the reason documented in the PR and notices?

## Reviewed product dependencies

- `@zip.js/zip.js` `2.8.26`, BSD-3-Clause: approved for the browser-local
  `/read-local` ZIP/CBZ image-opening route only. It is pinned, dynamically
  imported by the local reader, called with `useWebWorkers: false`, adds no
  native binary or network behavior, and no source code was copied into the
  repository.
- `@d-i-t-a/reader` `2.5.3`, Apache-2.0: approved only for the existing EPUB
  WebPub fallback. It replaces the historical project fork and its vulnerable
  bundled PDF.js 2.14.305 with the official release's PDF.js 5.5.207. The exact
  reader/PDF.js versions and EPUB initialization contract are test-covered.
  This does not authorize direct PDF.js use by ShelfView's PDF reader.

## Stop and escalate

Create an ADR and request owner review for:

- destructive migration
- auth model change
- source-media write access
- GPL/AGPL reuse
- direct PDF.js integration
- bundled third-party dictionary data
- server-side archive extraction strategy change
- client-side WebGL/Canvas image-processing pipeline
- user upload from the public internet
- framework-wide upgrade
