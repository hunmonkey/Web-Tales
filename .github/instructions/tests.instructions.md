---
name: ShelfView Tests
description: Test and validation rules
applyTo: "**/*Test*.kt,**/*.spec.ts,**/*.test.ts,**/__tests__/**,test-fixtures/**"
---

# Test rules

- Test user-visible behavior, boundaries, authorization, malformed input, and regression paths.
- Use deterministic fixtures with no copyrighted content.
- Archive attack fixtures must be structurally minimal, not genuinely huge.
- Include 0/1-based page boundary tests.
- Include fingerprint mismatch and Locator schema version tests.
- For TXT, cover UTF-8, BOM, euc-kr, CRLF, long lines, and XSS strings.
- For image directories, cover marker/no marker, natural sort, empty and nested folders, changed files, and forbidden image sizes.
- Never delete a failing test merely to make the suite green.
