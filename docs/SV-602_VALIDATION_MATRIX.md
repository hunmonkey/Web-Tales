# SV-602 Validation Matrix

This file tracks browser evidence for ShelfView's MVP reader goal.
It is a release checklist, not a replacement for `docs/AI_PROGRESS.md`.

## Scope

The owner goal is a self-hosted web reader that can open:

- standalone JPG/image fixed-page books
- standalone PDF books
- standalone TXT books
- image-directory books marked by `.shelfview-book`
- ZIP books that contain readable TXT, PDF, JPG and GIF entries

SV-602 currently requires browser validation on:

- desktop Chrome
- desktop Edge
- desktop Firefox

Mobile, iPhone/iOS-family and actual Android device validation are excluded from the SV-602 completion gate by owner direction on 2026-06-19. The existing Android-like emulation check remains useful as optional coverage, but it is not required for completion.

## Current Automated Evidence

| Requirement | Current evidence | Status |
| --- | --- | --- |
| Standalone JPG/image fixed pages | `komga-webui/scripts/shelfview-smoke-chrome.ps1` opens `/read-v2/book-fixed` in Chrome and Edge and verifies page counter, live status, image load and no horizontal overflow. | Covered by local automation |
| Standalone PDF fixed pages | The same smoke script opens `/read-v2/book-pdf` and verifies `/api/v1/books/book-pdf/pages/0?zero_based=true`. | Covered by local automation |
| Standalone TXT reader | The same smoke script opens `/read-v2/book-text`, verifies paragraph count/status, continuous mode and escaped literal `<script>not html</script>`. | Covered by local automation |
| Image-directory books | The same smoke script opens `/read-v2/book-folder` and verifies `/api/shelfview/v1/books/book-folder/image-directory/pages/0?zero_based=true`. Backend coverage includes `ShelfViewImageDirectoryPageControllerTest` and manifest coverage. | Covered by local automation |
| ZIP internal TXT | The same smoke script opens `/read-v2/book-mixed` and verifies the first mixed item renders as TXT. Backend coverage includes `ShelfViewMixedTextControllerTest`. | Covered by local automation |
| ZIP internal JPG | The same smoke script navigates from ZIP TXT to JPG and verifies `/api/shelfview/v1/books/book-mixed/mixed/jpg/pages/0?zero_based=true`. Backend coverage includes `ShelfViewMixedPageControllerTest`. | Covered by local automation |
| ZIP internal PDF | The same smoke script navigates to the ZIP PDF item and verifies `/api/shelfview/v1/books/book-mixed/mixed/pdf/pages/0?zero_based=true`. Backend coverage includes `ShelfViewMixedPageControllerTest`. | Covered by local automation |
| ZIP internal GIF | The same smoke script navigates to the ZIP GIF item and verifies `/api/shelfview/v1/books/book-mixed/mixed/gif/pages/0?zero_based=true`. Backend coverage includes GIF byte preservation in `ShelfViewMixedPageControllerTest`. | Covered by local automation |
| Desktop Chrome | `shelfview-smoke-chrome.ps1 -BrowserLabel Chrome` passes with `C:\Program Files\Google\Chrome\Application\chrome.exe`. | Covered by local automation |
| Desktop Edge | `shelfview-smoke-chrome.ps1 -ChromePath <msedge.exe> -BrowserLabel Edge` passes with `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`. | Covered by local automation |
| Android-like portrait/landscape rotation | The same smoke script uses Chrome DevTools mobile metrics, checks text portrait flow, changes from `390x844` to `844x390`, and verifies the current fixed page remains in view. | Optional coverage; not a completion gate |
| LAN-bind smoke server mode | `shelfview-smoke-chrome.ps1 -BindHost 0.0.0.0 -PublicHost 127.0.0.1 -BrowserLabel Chrome-LAN` passes locally, proving the script can bind the mock API and Vue dev server on all interfaces for later desktop browser checks. | Covered by local automation |
| Desktop Firefox | Firefox was not found in common Windows install paths or by `Get-Command firefox` in this environment. | Pending |

## Manual Desktop Browser Runbook

Use this when Firefox or another desktop browser is available on the same LAN as this development PC.

1. Find the PC IPv4 address on the LAN.

   ```powershell
   Get-NetIPAddress -AddressFamily IPv4 |
     Where-Object { $_.IPAddress -notlike '127.*' -and $_.PrefixOrigin -ne 'WellKnown' } |
     Select-Object IPAddress, InterfaceAlias
   ```

2. Start the smoke mock servers on all network interfaces and keep them running.

   ```powershell
   $node18 = Join-Path $env:TEMP 'codex-tools\node-v18.20.8-win-x64\node.exe'
   $pcIp = '<PC-LAN-IP>'
   powershell.exe -NoProfile -ExecutionPolicy Bypass `
     -File .\komga-webui\scripts\shelfview-smoke-chrome.ps1 `
     -NodePath $node18 `
     -BindHost 0.0.0.0 `
     -PublicHost $pcIp `
     -BrowserLabel Chrome `
     -KeepServers
   ```

3. Open these URLs on the target desktop browser:

   - `http://<PC-LAN-IP>:8094/read-v2/book-fixed`
   - `http://<PC-LAN-IP>:8094/read-v2/book-pdf`
   - `http://<PC-LAN-IP>:8094/read-v2/book-text`
   - `http://<PC-LAN-IP>:8094/read-v2/book-folder`
   - `http://<PC-LAN-IP>:8094/read-v2/book-mixed`

4. Record pass/fail evidence in `docs/AI_PROGRESS.md`.

For each URL, verify:

- the reader loads without an error panel
- the page or text is visible
- the page/paragraph counter matches the content
- there is no unexpected horizontal page overflow
- next/previous controls work
- on `book-mixed`, next navigation reaches TXT, JPG, PDF and GIF items in order

If a firewall blocks the target desktop browser, stop the kept servers and rerun after allowing local network access for Node.js.

## Completion Rule

Do not mark SV-602 complete until the pending desktop Firefox check has direct evidence recorded in `docs/AI_PROGRESS.md`, or until the owner explicitly excludes Firefox from the completion gate too.
