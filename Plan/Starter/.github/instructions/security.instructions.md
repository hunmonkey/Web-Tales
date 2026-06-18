---
name: ShelfView Security
description: Security rules for reader content, archives and APIs
applyTo: "**"
---

# Security rules

- Treat PDF, TXT, archives, images, metadata, filenames, labels, notes, and preference JSON as untrusted.
- Authenticate and authorize all content and user-data endpoints.
- Reject path traversal, absolute paths, symlink archive entries, nested archives by default, excessive entry count, excessive expanded size, abnormal compression ratio, and excessive pixel count.
- Enforce configurable limits and test every rejection path with small fixtures.
- Escape all text output and avoid dynamic HTML.
- Never write derived files into source media directories.
- Never commit secrets or include them in Docker layers.
- Report any design that weakens isolation before implementing it.
