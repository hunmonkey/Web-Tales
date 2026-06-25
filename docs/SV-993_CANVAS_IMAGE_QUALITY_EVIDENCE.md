# SV-993 Canvas Image-Quality Browser Evidence

Date: 2026-06-25

## Scope

SV-993 validates the existing `CANVAS_SHARPEN` prototype before any heavier
WebGL, background-cleanup or adaptive monochrome work is considered.

This pass does not enable Canvas sharpen as a primary user-facing option and
does not add WebGL, AI upscaling, derived-image caching, source-media writes or
new dependencies.

## Evidence Added

- `tales-webui/scripts/shelfview-smoke-chrome.ps1` now checks:
  - normal fixed-page rendering still works;
  - `CANVAS_SHARPEN` can render a 480 x 720 page as a real `<canvas>`;
  - the Canvas pixels are readable when the mock API explicitly allows
    credentials-enabled CORS;
  - an over-budget 4000 x 4000 image falls back to a loaded `<img>` instead of
    trying to process too many pixels;
  - existing desktop, mobile-emulated, PDF, image-directory and mixed ZIP smoke
    coverage still passes.
- `ShelfViewCanvasImagePage` now marks canvas/fallback diagnostics with
  `data-canvas-src` and `data-canvas-fallback` so browser smoke evidence can
  distinguish processed Canvas from fallback image rendering.
- `ShelfViewCanvasImagePage` loads Canvas images with `crossOrigin =
  'use-credentials'`; this lets browsers read Canvas pixels only when the
  server explicitly allows that origin and credentials. If Canvas loading or
  pixel access fails, the existing fallback `<img>` path remains in use.

## Browser Results

| Browser | Result | Evidence |
| --- | --- | --- |
| Chrome 149 headless | Passed | Canvas sharpen rendered as `canvas`, 480 x 720, readable pixel sample, no horizontal overflow |
| Edge 149 headless | Passed | Canvas sharpen rendered as `canvas`, 480 x 720, readable pixel sample, no horizontal overflow |

Both runs also passed the over-budget fallback check:

- route: `/read-v2/book-canvas-big`
- image size: 4000 x 4000
- expected result: `img` fallback with `data-canvas-fallback="true"`
- observed result: passed in Chrome and Edge

## Commands Run

```powershell
npm.cmd run test:unit -- --runTestsByPath tests/unit/shelfview/ShelfViewCanvasImagePage.spec.ts tests/unit/shelfview/canvas-image-processing.spec.ts tests/unit/shelfview/ShelfViewReaderShell.spec.ts
npx.cmd eslint src/shelfview/components/ShelfViewCanvasImagePage.vue tests/unit/shelfview/ShelfViewCanvasImagePage.spec.ts tests/unit/shelfview/canvas-image-processing.spec.ts tests/unit/shelfview/ShelfViewReaderShell.spec.ts
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tales-webui\scripts\shelfview-smoke-chrome.ps1 -BrowserLabel Chrome
$edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"; powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tales-webui\scripts\shelfview-smoke-chrome.ps1 -ChromePath $edge -BrowserLabel Edge
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command '$parseErrors = $null; [System.Management.Automation.PSParser]::Tokenize((Get-Content -Raw -Path "tales-webui\scripts\shelfview-smoke-chrome.ps1"), [ref]$parseErrors) | Out-Null; if ($parseErrors.Count -gt 0) { $parseErrors | ForEach-Object { Write-Error $_.Message }; exit 1 }; "PowerShell parse ok"'
SHELFVIEW_SKIP_TSCHECK=true npm.cmd run build --userconfig=NUL
git diff --check
```

## Remaining Boundaries

- This evidence pass alone did not approve a WebGL path, background cleanup,
  denoise, adaptive monochrome or a derived-image cache.
- On 2026-06-25, the owner separately approved a browser-local follow-up for
  `WEBGL_SHARPEN`, `BACKGROUND_CLEANUP` and `ADAPTIVE_MONO`; that follow-up
  keeps source media read-only and does not add a derived-image cache.
- A future image-processing mode still needs its own named transform, fallback
  behavior, browser evidence and dependency/license review if a package is
  proposed.
