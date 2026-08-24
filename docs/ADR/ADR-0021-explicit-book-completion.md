# ADR-0021: Finishing a book is an explicit action

- Status: Accepted
- Date: 2026-08-24
- Ticket: SV-1282
- Related: `docs/BACKLOG.md`, `docs/AI_PROGRESS.md`,
  `tales/src/main/kotlin/org/tales/domain/service/BookLifecycle.kt`,
  `tales-webui/src/shelfview/views/ShelfViewReaderShell.vue`

## Context

"Keep reading" is the reader's home list. It loads books whose read status is
`IN_PROGRESS`, which the search resolves as "has read progress and is not
completed":

```kotlin
val progress = ReadProgress(book.id, user.id, page, page == media.pageCount, locator = locator)
```

Every position save runs through that line. The ShelfView reader mirrors its
fixed-page position into Tales read progress on a two-second autosave, so
turning to the last page of a book finished it, and the book left keep reading
at that moment. The same rule existed twice more, in the Readium progression
path used for EPUB and for page-based progressions.

The owner's report: a book must not leave keep reading until they say it is
finished. Nothing in the reader said so — the end-of-book prompt's "read next
volume" and "exit" buttons called `markReadProgressCompleted` as a side effect,
and neither is labelled as finishing anything.

## Decision

**Reading a book to its end no longer finishes it. Only an explicit action
does.**

- `BookLifecycle.markReadProgress` — both the page overload and the
  `R2Progression` overload, including its EPUB branch — always stores
  `completed = false`. The owner chose this over confining the change to the
  ShelfView reader, so that no client and no format is an exception.
- `markReadProgressCompleted` is unchanged and is now the only thing that
  completes a book. It is reached from `PATCH /api/v1/books/{id}/read-progress`
  with `{"completed": true}`, which is what the book menu's "mark as read" and
  the reader's new **finish** button send.
- The reader's end-of-book prompt gains that finish button. "Read next volume"
  and "exit" no longer complete the book, so the prompt's three existing
  buttons all leave the book in keep reading.
- Once the reader has finished a book it stops writing positions for it, for
  the rest of that reader session. A position save mirrors as an unfinished
  book, so a later save — `closeBook` runs one, and so do `pagehide` and
  `visibilitychange` — would undo the finish that was just asked for.

## Consequences

- Keep reading holds every book that has been opened until it is finished. That
  is the requested behaviour and it is also the whole change: no new state, no
  schema change, no new endpoint.
- Read status is now a statement by the reader rather than a measurement.
  "Recently read", on deck, and a series' read/unread counts all follow explicit
  finishes from here on, and will look emptier than before until books are
  finished.
- Public API behaviour changes without the API changing shape.
  `PATCH /api/v1/books/{id}/read-progress` with a page no longer completes a
  book at the last page. Callers that relied on that must send
  `{"completed": true}`. This is a deliberate divergence from upstream and will
  conflict on an upstream merge of `BookLifecycle`.
- Books already completed before this change stay completed and stay out of
  keep reading. Reopening one and turning a page returns it, because a position
  save now writes `completed = false`.
- Reversible: restoring the three expressions restores the old behaviour. No
  data is migrated or destroyed either way.

## Alternatives considered

- **Confine the change to the ShelfView reader** by passing a flag from
  `ShelfViewPositionController`. Smaller blast radius and it keeps `/api/v1`
  behaving as upstream does, but EPUB books read through the bridge would still
  vanish on their own. The owner chose the consistent rule.
- **Add a separate ShelfView "finished" flag** and leave Tales' completion
  alone, letting keep reading list completed-but-not-finished books. This keeps
  upstream semantics exactly, at the cost of a Flyway migration, a new endpoint,
  a second notion of "done" for a reader to understand, and two states that can
  disagree. Rejected as more machinery than the request needs.
