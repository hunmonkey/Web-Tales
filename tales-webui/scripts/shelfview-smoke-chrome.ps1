param(
  [string] $ChromePath = "",
  [string] $NodePath = "node",
  [string] $BrowserLabel = "Chrome",
  [string] $BindHost = "127.0.0.1",
  [string] $PublicHost = "127.0.0.1",
  [int] $FrontendPort = 8094,
  [int] $MockApiPort = 8095,
  [int] $CdpPort = 9227,
  [switch] $KeepServers
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$webuiDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$healthHost = if ($BindHost -eq "0.0.0.0") { "127.0.0.1" } else { $BindHost }
$frontendUrl = "http://$PublicHost`:$FrontendPort"
$mockApiUrl = "http://$PublicHost`:$MockApiPort"
$frontendHealthUrl = "http://$healthHost`:$FrontendPort"
$mockApiHealthUrl = "http://$healthHost`:$MockApiPort"
$tempRoot = Join-Path $env:TEMP "shelfview-smoke-chrome"
$frontendLog = Join-Path $tempRoot "frontend.log"
$frontendErr = Join-Path $tempRoot "frontend.err.log"
$mockApiLog = Join-Path $tempRoot "mock-api.log"
$mockApiErr = Join-Path $tempRoot "mock-api.err.log"
$chromeUserData = Join-Path $tempRoot "chrome-profile"

New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
Remove-Item $frontendLog, $frontendErr, $mockApiLog, $mockApiErr -ErrorAction SilentlyContinue
Remove-Item $chromeUserData -Recurse -Force -ErrorAction SilentlyContinue

$startedProcesses = New-Object System.Collections.Generic.List[System.Diagnostics.Process]
$ws = $null

function Get-ListenerProcessId {
  param([int] $Port)

  $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($connection) {
    return [int] $connection.OwningProcess
  }

  return $null
}

function Assert-PortFree {
  param([int] $Port)

  $listenerPid = Get-ListenerProcessId -Port $Port
  if ($listenerPid) {
    throw "Port $Port is already in use by process $listenerPid."
  }
}

function Find-Chrome {
  if ($ChromePath) {
    if (Test-Path $ChromePath) {
      return (Resolve-Path $ChromePath).Path
    }

    throw "Browser executable was not found at $ChromePath."
  }

  $candidates = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"
  )

  foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path $candidate)) {
      return $candidate
    }
  }

  throw "Chrome or Edge was not found. Pass -ChromePath to an installed Chromium browser."
}

function Resolve-Executable {
  param(
    [string] $Command,
    [string] $DisplayName
  )

  if ($Command -and (Test-Path $Command)) {
    return (Resolve-Path $Command).Path
  }

  $resolved = Get-Command $Command -ErrorAction SilentlyContinue
  if ($resolved) {
    return $resolved.Source
  }

  throw "$DisplayName was not found. Pass -$($DisplayName)Path to an installed executable."
}

function Stop-ProcessTree {
  param([int] $ProcessId)

  $children = Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" -ErrorAction SilentlyContinue
  foreach ($child in $children) {
    Stop-ProcessTree -ProcessId ([int] $child.ProcessId)
  }

  Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Wait-HttpOk {
  param(
    [string] $Url,
    [int] $TimeoutSeconds = 240
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
      if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
        return
      }
    } catch {
      Start-Sleep -Seconds 2
    }
  }

  throw "Timed out waiting for $Url."
}

function Start-MockApi {
  Assert-PortFree -Port $MockApiPort

  $mockScript = @'
const http = require('http');
const url = require('url');
let currentReq = null;
const frontendOrigin = '__FRONTEND_URL__';
const bindHost = '__BIND_HOST__';
const port = __MOCK_API_PORT__;
const svgPage = (label) => Buffer.from(`<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"480\" height=\"720\" viewBox=\"0 0 480 720\"><rect width=\"480\" height=\"720\" fill=\"#f7f2df\"/><rect x=\"36\" y=\"36\" width=\"408\" height=\"648\" fill=\"#ffffff\" stroke=\"#1d4ed8\" stroke-width=\"8\"/><text x=\"240\" y=\"330\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"42\" fill=\"#111827\">${label}</text><text x=\"240\" y=\"390\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"22\" fill=\"#374151\">ShelfView browser smoke</text></svg>`);
const largeSvgPage = (label) => Buffer.from(`<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"4000\" height=\"4000\" viewBox=\"0 0 4000 4000\"><rect width=\"4000\" height=\"4000\" fill=\"#f7f2df\"/><rect x=\"240\" y=\"240\" width=\"3520\" height=\"3520\" fill=\"#ffffff\" stroke=\"#1d4ed8\" stroke-width=\"40\"/><text x=\"2000\" y=\"1940\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"160\" fill=\"#111827\">${label}</text><text x=\"2000\" y=\"2120\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"80\" fill=\"#374151\">over canvas smoke budget</text></svg>`);
function headers(extra = {}) {
  const origin = currentReq?.headers?.origin || frontendOrigin;
  return Object.assign({
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Credentials': 'true',
    'Access-Control-Allow-Headers': 'content-type,authorization,x-requested-with',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
    'Vary': 'Origin'
  }, extra);
}
function json(res, status, body) {
  res.writeHead(status, headers({'Content-Type': 'application/json; charset=utf-8'}));
  res.end(JSON.stringify(body));
}
function empty(res, status = 204) {
  res.writeHead(status, headers());
  res.end();
}
function textContent(bookId, fingerprint, paragraphs) {
  return {schemaVersion: 1, bookId, contentFingerprint: fingerprint, encoding: 'UTF-8', normalizationVersion: 1, paragraphs};
}
const user = {id: 'user-1', email: 'sv602@example.org', roles: ['ADMIN', 'PAGE_STREAMING', 'FILE_DOWNLOAD'], sharedAllLibraries: true, sharedLibrariesIds: [], labelsAllow: [], labelsExclude: []};
const library = {id: 'library-1', name: 'SV602 Library', root: 'mock', importComicInfoBook: false, importComicInfoSeries: false, importComicInfoCollection: false, importComicInfoReadList: false, importEpubBook: false, importEpubSeries: false, importMylarSeries: false, importLocalArtwork: false, scanForceModifiedTime: false, scanDeep: false, scanOnStartup: false, scanCbx: true, scanPdf: true, scanEpub: true, scanTxt: true, scanImageDirectory: true, oneshotsDirectory: '', unavailable: false, unpinned: false};
function book(id) {
  if (id === 'book-text') return {id, seriesId: 'series-1', name: id, oneshot: false, media: {status: 'READY', mediaType: 'text/plain', mediaProfile: 'TXT', pagesCount: 0, comment: '', epubDivinaCompatible: false, epubIsKepub: false}, metadata: {title: 'TXT Smoke', number: '1'}};
  if (id === 'book-pdf') return {id, seriesId: 'series-1', name: id, oneshot: false, media: {status: 'READY', mediaType: 'application/pdf', mediaProfile: 'PDF', pagesCount: 2, comment: '', epubDivinaCompatible: false, epubIsKepub: false}, metadata: {title: 'PDF Smoke', number: '1'}};
  if (id === 'book-folder') return {id, seriesId: 'series-1', name: id, oneshot: false, media: {status: 'READY', mediaType: 'inode/directory', mediaProfile: 'IMAGE_DIRECTORY', pagesCount: 2, comment: '', epubDivinaCompatible: false, epubIsKepub: false}, metadata: {title: 'Image Folder Smoke', number: '1'}};
  if (id === 'book-canvas-big') return {id, seriesId: 'series-1', name: id, oneshot: false, media: {status: 'READY', mediaType: 'application/zip', mediaProfile: 'DIVINA', pagesCount: 1, comment: '', epubDivinaCompatible: false, epubIsKepub: false}, metadata: {title: 'Canvas Big Smoke', number: '1'}};
  return {id, seriesId: 'series-1', name: id, oneshot: false, media: {status: 'READY', mediaType: 'application/zip', mediaProfile: 'DIVINA', pagesCount: 3, comment: '', epubDivinaCompatible: false, epubIsKepub: false}, metadata: {title: id === 'book-mixed' ? 'Mixed ZIP Smoke' : 'JPG Smoke', number: '1'}};
}
const series = {id: 'series-1', metadata: {title: 'SV602 Browser Series'}};
const fixedManifest = {schemaVersion: 1, bookId: 'book-fixed', title: 'JPG Smoke', kind: 'FIXED_PAGE_SEQUENCE', sourceFormat: 'JPG', contentFingerprint: 'tales-file-state:book-fixed', pageCount: 3, pages: {endpointTemplate: '/api/v1/books/book-fixed/pages/{page}?zero_based=true'}, capabilities: {search: false, textSelection: false, doublePage: true, continuousScroll: true, rtl: true, imageFilters: true}};
const canvasBigManifest = {schemaVersion: 1, bookId: 'book-canvas-big', title: 'Canvas Big Smoke', kind: 'FIXED_PAGE_SEQUENCE', sourceFormat: 'JPG', contentFingerprint: 'tales-file-state:book-canvas-big', pageCount: 1, pages: {endpointTemplate: '/api/v1/books/book-canvas-big/pages/{page}?zero_based=true'}, capabilities: {search: false, textSelection: false, doublePage: false, continuousScroll: false, rtl: false, imageFilters: true}};
const pdfManifest = {schemaVersion: 1, bookId: 'book-pdf', title: 'PDF Smoke', kind: 'FIXED_PAGE_SEQUENCE', sourceFormat: 'PDF', contentFingerprint: 'tales-file-state:book-pdf', pageCount: 2, pages: {endpointTemplate: '/api/v1/books/book-pdf/pages/{page}?zero_based=true'}, capabilities: {search: false, textSelection: false, doublePage: true, continuousScroll: true, rtl: false, imageFilters: true}};
const folderManifest = {schemaVersion: 1, bookId: 'book-folder', title: 'Image Folder Smoke', kind: 'FIXED_PAGE_SEQUENCE', sourceFormat: 'IMAGE_DIRECTORY', contentFingerprint: 'tales-file-state:book-folder', pageCount: 2, pages: {endpointTemplate: '/api/shelfview/v1/books/book-folder/image-directory/pages/{page}?zero_based=true'}, capabilities: {search: false, textSelection: false, doublePage: true, continuousScroll: true, rtl: false, imageFilters: true}};
const textManifest = {schemaVersion: 1, bookId: 'book-text', title: 'TXT Smoke', kind: 'TEXT_REFLOW', sourceFormat: 'TXT', contentFingerprint: 'tales-file-state:book-text', text: {endpoint: '/api/shelfview/v1/books/book-text/text', detectedEncoding: 'UTF-8', normalizationVersion: 1}, capabilities: {search: false, textSelection: true, doublePage: false, continuousScroll: true, rtl: false, imageFilters: false}};
const mixedManifest = {schemaVersion: 1, bookId: 'book-mixed', title: 'Mixed ZIP Smoke', kind: 'MIXED_CONTENT_SEQUENCE', sourceFormat: 'ZIP', contentFingerprint: 'tales-file-state:book-mixed', pageCount: 5, items: [{itemId: 'txt', index: 0, kind: 'TEXT_REFLOW', label: '001.txt', contentFingerprint: 'tales-file-state:book-mixed:txt', sourceFormat: 'TXT', text: {endpoint: '/api/shelfview/v1/books/book-mixed/mixed/txt/text', detectedEncoding: 'UTF-8', normalizationVersion: 1}}, {itemId: 'jpg', index: 1, kind: 'FIXED_PAGE_SEQUENCE', label: '002.jpg', contentFingerprint: 'tales-file-state:book-mixed:jpg', sourceFormat: 'JPG', pageCount: 1, pages: {endpointTemplate: '/api/shelfview/v1/books/book-mixed/mixed/jpg/pages/{page}?zero_based=true'}}, {itemId: 'pdf', index: 2, kind: 'FIXED_PAGE_SEQUENCE', label: '003.pdf', contentFingerprint: 'tales-file-state:book-mixed:pdf', sourceFormat: 'PDF', pageCount: 2, pages: {endpointTemplate: '/api/shelfview/v1/books/book-mixed/mixed/pdf/pages/{page}?zero_based=true'}}, {itemId: 'gif', index: 3, kind: 'FIXED_PAGE_SEQUENCE', label: '004.gif', contentFingerprint: 'tales-file-state:book-mixed:gif', sourceFormat: 'GIF', pageCount: 1, pages: {endpointTemplate: '/api/shelfview/v1/books/book-mixed/mixed/gif/pages/{page}?zero_based=true'}}], capabilities: {search: false, textSelection: true, doublePage: true, continuousScroll: true, rtl: false, imageFilters: true}};
const server = http.createServer((req, res) => {
  currentReq = req;
  const parsed = url.parse(req.url, true);
  const path = parsed.pathname;
  console.log(req.method, path);
  if (req.method === 'OPTIONS') return empty(res, 204);
  if (path === '/api/v2/users/me') return json(res, 200, user);
  if (path === '/api/v1/libraries') return json(res, 200, [library]);
  if (path === '/api/v1/client-settings/global/list' || path === '/api/v1/client-settings/user/list') return json(res, 200, {});
  if (path === '/api/v1/claim') return json(res, 200, {claimed: true});
  if (path.startsWith('/api/v1/books/')) {
    const parts = path.split('/');
    const id = parts[4];
    if (parts[5] === 'pages') {
      const page = parts[6] || '0';
      res.writeHead(200, headers({'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store'}));
      if (id === 'book-canvas-big') return res.end(largeSvgPage(`${id} page ${Number(page) + 1}`));
      return res.end(svgPage(`${id} page ${Number(page) + 1}`));
    }
    return json(res, 200, book(id));
  }
  if (path === '/api/v1/series/series-1') return json(res, 200, series);
  if (path === '/api/shelfview/v1/books/book-fixed/manifest') return json(res, 200, fixedManifest);
  if (path === '/api/shelfview/v1/books/book-canvas-big/manifest') return json(res, 200, canvasBigManifest);
  if (path === '/api/shelfview/v1/books/book-pdf/manifest') return json(res, 200, pdfManifest);
  if (path === '/api/shelfview/v1/books/book-folder/manifest') return json(res, 200, folderManifest);
  if (path === '/api/shelfview/v1/books/book-text/manifest') return json(res, 200, textManifest);
  if (path === '/api/shelfview/v1/books/book-mixed/manifest') return json(res, 200, mixedManifest);
  if (path === '/api/shelfview/v1/books/book-text/text') return json(res, 200, textContent('book-text', 'tales-file-state:book-text', ['TXT first paragraph for browser smoke.', '<script>not html</script>', 'TXT last paragraph after resize.']));
  if (path === '/api/shelfview/v1/books/book-mixed/mixed/txt/text') return json(res, 200, textContent('book-mixed', 'tales-file-state:book-mixed:txt', ['ZIP TXT opening paragraph.', 'ZIP JPG follows after next.']));
  if (path === '/api/shelfview/v1/books/book-mixed/mixed/jpg/pages/0') {
    res.writeHead(200, headers({'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store'}));
    return res.end(svgPage('mixed jpg page'));
  }
  if (path === '/api/shelfview/v1/books/book-mixed/mixed/pdf/pages/0' || path === '/api/shelfview/v1/books/book-mixed/mixed/pdf/pages/1') {
    const page = path.endsWith('/1') ? 2 : 1;
    res.writeHead(200, headers({'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store'}));
    return res.end(svgPage(`mixed pdf page ${page}`));
  }
  if (path === '/api/shelfview/v1/books/book-folder/image-directory/pages/0' || path === '/api/shelfview/v1/books/book-folder/image-directory/pages/1') {
    const page = path.endsWith('/1') ? 2 : 1;
    res.writeHead(200, headers({'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store'}));
    return res.end(svgPage(`folder page ${page}`));
  }
  if (path === '/api/shelfview/v1/books/book-mixed/mixed/gif/pages/0') {
    res.writeHead(200, headers({'Content-Type': 'image/svg+xml', 'Cache-Control': 'no-store'}));
    return res.end(svgPage('mixed gif page'));
  }
  if (path.endsWith('/position') && req.method === 'GET') return empty(res, 204);
  if (path.endsWith('/position') && req.method === 'PUT') return json(res, 200, {locator: {schemaVersion: 1, kind: 'PAGE', page: 0, offset: 0, contentFingerprint: 'mock'}, progression: 0, contentFingerprint: 'mock', updatedAt: new Date().toISOString()});
  if (path.endsWith('/bookmarks')) return json(res, 200, []);
  if (path === '/api/shelfview/v1/preferences' && req.method === 'GET') return json(res, 200, {scopeType: 'BOOK', scopeId: 'book-fixed', format: 'FIXED_PAGE_SEQUENCE', effectiveSettings: {layout: 'SINGLE_PAGE', readingDirection: 'LTR', pageFit: 'SCREEN', imageFilterPreset: 'ORIGINAL'}, sources: []});
  if (path === '/api/shelfview/v1/preferences' && req.method === 'PUT') return json(res, 200, {scopeType: 'BOOK', scopeId: 'book-fixed', format: 'FIXED_PAGE_SEQUENCE', effectiveSettings: {layout: 'SINGLE_PAGE', readingDirection: 'LTR', pageFit: 'SCREEN', imageFilterPreset: 'ORIGINAL'}, sources: []});
  return json(res, 404, {message: 'mock not found', path});
});
server.listen(port, bindHost, () => console.log(`mock api ready http://${bindHost}:${port}`));
'@
  $mockScript = $mockScript.Replace("__FRONTEND_URL__", $frontendUrl).Replace("__BIND_HOST__", $BindHost).Replace("__MOCK_API_PORT__", [string] $MockApiPort)

  $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($mockScript))
  $bootstrap = "eval(Buffer.from('$encoded','base64').toString('utf8'))"
  $resolvedNode = Resolve-Executable -Command $NodePath -DisplayName "Node"
  $process = Start-Process -FilePath $resolvedNode -ArgumentList @("-e", $bootstrap) -RedirectStandardOutput $mockApiLog -RedirectStandardError $mockApiErr -PassThru -WindowStyle Hidden
  $startedProcesses.Add($process)
  Wait-HttpOk -Url "$mockApiHealthUrl/api/v2/users/me" -TimeoutSeconds 30
  return $process
}

function Start-Frontend {
  Assert-PortFree -Port $FrontendPort

  $oldReader = $env:VUE_APP_SHELFVIEW_READER
  $oldApi = $env:VUE_APP_TALES_API_URL
  $oldPath = $env:PATH
  try {
    $resolvedNode = Resolve-Executable -Command $NodePath -DisplayName "Node"
    $cliPath = Join-Path $webuiDir "node_modules\@vue\cli-service\bin\vue-cli-service.js"
    if (-not (Test-Path $cliPath)) {
      throw "Vue CLI service was not found. Run npm install in $webuiDir first."
    }

    $env:PATH = "$(Split-Path -Parent $resolvedNode);$oldPath"
    $env:VUE_APP_SHELFVIEW_READER = "true"
    $env:VUE_APP_TALES_API_URL = $mockApiUrl
    $process = Start-Process -FilePath $resolvedNode -ArgumentList @($cliPath, "serve", "--port", [string] $FrontendPort, "--host", $BindHost) -WorkingDirectory $webuiDir -RedirectStandardOutput $frontendLog -RedirectStandardError $frontendErr -PassThru -WindowStyle Hidden
  } finally {
    $env:VUE_APP_SHELFVIEW_READER = $oldReader
    $env:VUE_APP_TALES_API_URL = $oldApi
    $env:PATH = $oldPath
  }

  $startedProcesses.Add($process)
  Wait-HttpOk -Url "$frontendHealthUrl/" -TimeoutSeconds 420
  return $process
}

function Start-Chrome {
  $resolvedChrome = Find-Chrome
  $script:browserExecutable = $resolvedChrome
  Assert-PortFree -Port $CdpPort

  $process = Start-Process -FilePath $resolvedChrome -ArgumentList @(
    "--headless=new",
    "--disable-gpu",
    "--no-first-run",
    "--no-default-browser-check",
    "--remote-debugging-port=$CdpPort",
    "--user-data-dir=$chromeUserData",
    "about:blank"
  ) -PassThru -WindowStyle Hidden
  $startedProcesses.Add($process)

  $deadline = (Get-Date).AddSeconds(20)
  while ((Get-Date) -lt $deadline) {
    try {
      return Invoke-RestMethod -Uri "http://127.0.0.1:$CdpPort/json/version" -TimeoutSec 2
    } catch {
      Start-Sleep -Milliseconds 500
    }
  }

  throw "Chrome DevTools Protocol did not start on port $CdpPort."
}

function Connect-Cdp {
  $target = Invoke-RestMethod -Method Put -Uri "http://127.0.0.1:$CdpPort/json/new?about:blank" -TimeoutSec 5
  $script:ws = [System.Net.WebSockets.ClientWebSocket]::new()
  $script:ws.ConnectAsync([Uri] $target.webSocketDebuggerUrl, [Threading.CancellationToken]::None).Wait()
  $script:cdpId = 0
}

function Receive-CdpJson {
  $buffer = New-Object byte[] 1048576
  $segment = [ArraySegment[byte]]::new($buffer)
  $builder = [System.Text.StringBuilder]::new()

  do {
    $result = $script:ws.ReceiveAsync($segment, [Threading.CancellationToken]::None).Result
    if ($result.Count -gt 0) {
      [void] $builder.Append([Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count))
    }
  } while (-not $result.EndOfMessage)

  $text = $builder.ToString()
  if ($text.Length -eq 0) {
    return $null
  }

  return $text | ConvertFrom-Json
}

function Send-Cdp {
  param(
    [string] $Method,
    [hashtable] $Params = @{}
  )

  $script:cdpId += 1
  $id = $script:cdpId
  $payload = @{ id = $id; method = $Method; params = $Params } | ConvertTo-Json -Depth 80 -Compress
  $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
  $script:ws.SendAsync([ArraySegment[byte]]::new($bytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, [Threading.CancellationToken]::None).Wait()

  while ($true) {
    $message = Receive-CdpJson
    $messageId = $null
    if ($message) {
      $idProperty = $message.PSObject.Properties["id"]
      if ($idProperty) {
        $messageId = $idProperty.Value
      }
    }

    if ($message -and $messageId -eq $id) {
      return $message
    }
  }
}

function Invoke-CdpExpression {
  param([string] $Expression)

  $response = Send-Cdp -Method "Runtime.evaluate" -Params @{
    expression = $Expression
    awaitPromise = $true
    returnByValue = $true
  }

  return $response.result.result.value
}

function Set-Viewport {
  param(
    [int] $Width,
    [int] $Height,
    [bool] $Mobile,
    [string] $DesktopUserAgent
  )

  [void] (Send-Cdp -Method "Emulation.setDeviceMetricsOverride" -Params @{
      width = $Width
      height = $Height
      deviceScaleFactor = 1
      mobile = $Mobile
    })

  if ($Mobile) {
    [void] (Send-Cdp -Method "Network.setUserAgentOverride" -Params @{
        userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        platform = "Android"
      })
  } else {
    [void] (Send-Cdp -Method "Network.setUserAgentOverride" -Params @{
        userAgent = $DesktopUserAgent
        platform = "Windows"
      })
  }
}

function Wait-Reader {
  param([string] $Url)

  $state = $null

  for ($attempt = 1; $attempt -le 2; $attempt++) {
    [void] (Send-Cdp -Method "Page.navigate" -Params @{ url = $Url })
    $deadline = (Get-Date).AddSeconds(25)

    do {
      Start-Sleep -Milliseconds 500
      $state = Invoke-CdpExpression "({url: location.href, hasReader: !!document.querySelector('[data-testid=shelfview-reader-stage]'), error: document.querySelector('[data-testid=shelfview-reader-error]')?.innerText || null, text: document.body.innerText.slice(0, 250)})"
      if ($state.hasReader -or $state.error) {
        break
      }
    } while ((Get-Date) -lt $deadline)

    if ($state.hasReader) {
      break
    }
  }

  if (-not $state.hasReader) {
    throw "Reader did not load for $Url. Current URL: $($state.url). Error: $($state.error). Text: $($state.text)"
  }
}

function Assert-Smoke {
  param(
    [bool] $Condition,
    [string] $Message
  )

  if (-not $Condition) {
    throw $Message
  }
}

try {
  Assert-PortFree -Port $CdpPort

  $mockApiProcess = Start-MockApi
  $frontendProcess = Start-Frontend
  $chromeVersion = Start-Chrome
  Connect-Cdp

  [void] (Send-Cdp -Method "Page.enable")
  [void] (Send-Cdp -Method "Runtime.enable")
  [void] (Send-Cdp -Method "Network.enable")

  $desktopUserAgent = $chromeVersion."User-Agent"
  $results = @()

  Set-Viewport -Width 1366 -Height 768 -Mobile $false -DesktopUserAgent $desktopUserAgent
  Wait-Reader -Url "$frontendUrl/read-v2/book-fixed"
  $desktopFixed = Invoke-CdpExpression @"
(() => {
  const rect = el => {
    if (!el) return null;
    const b = el.getBoundingClientRect();
    return {left: b.left, top: b.top, right: b.right, bottom: b.bottom, width: b.width, height: b.height};
  };
  const page = document.querySelector('[data-testid="shelfview-reader-page"]');
  return {
    name: 'desktop fixed page',
    url: location.href,
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    live: document.querySelector('[data-testid="shelfview-reader-live-status"]')?.innerText.trim() || '',
    pageVisible: !!page,
    pageComplete: page ? page.complete : false,
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    stage: rect(document.querySelector('[data-testid="shelfview-reader-stage"]')),
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($desktopFixed.counter -eq "1 / 3") "Desktop fixed-page counter was '$($desktopFixed.counter)', not 1 / 3."
  Assert-Smoke ($desktopFixed.live -eq "Page 1 of 3") "Desktop fixed-page live status was '$($desktopFixed.live)', not Page 1 of 3."
  Assert-Smoke ($desktopFixed.pageVisible -and $desktopFixed.pageComplete) "Desktop fixed-page image was not loaded."
  Assert-Smoke (-not $desktopFixed.horizontalOverflow) "Desktop fixed-page reader has horizontal overflow."
  $results += $desktopFixed

  $canvasSharpen = Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  const findReaderVm = () => {
    const nodes = Array.from(document.querySelectorAll('*'));
    for (const node of nodes) {
      let vm = node.__vue__;
      while (vm) {
        if (vm.`$options?.name === 'ShelfViewReaderShell') return vm;
        vm = vm.`$parent;
      }
    }
    return null;
  };
  const waitFor = async predicate => {
    const deadline = Date.now() + 8000;
    while (Date.now() < deadline) {
      const value = predicate();
      if (value) return value;
      await wait(100);
    }
    return predicate();
  };
  const vm = findReaderVm();
  if (!vm) return {name: 'desktop canvas sharpen', vmFound: false};
  vm.setImageFilterPreset('CANVAS_SHARPEN');
  await vm.`$nextTick();
  const page = await waitFor(() => {
    const candidate = document.querySelector('[data-testid="shelfview-reader-page"]');
    return candidate && candidate.tagName.toLowerCase() === 'canvas' && candidate.width === 480 && candidate.height === 720 ? candidate : null;
  });
  let pixelReadable = false;
  let sample = [];
  if (page) {
    try {
      const context = page.getContext('2d');
      sample = Array.from(context.getImageData(Math.floor(page.width / 2), Math.floor(page.height / 2), 1, 1).data);
      pixelReadable = true;
    } catch (_error) {
      pixelReadable = false;
    }
  }
  return {
    name: 'desktop canvas sharpen',
    url: location.href,
    vmFound: true,
    tagName: page?.tagName.toLowerCase() || '',
    canvasWidth: page?.width || 0,
    canvasHeight: page?.height || 0,
    canvasSrc: page?.getAttribute('data-canvas-src') || '',
    pixelReadable,
    sample,
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($canvasSharpen.vmFound) "Canvas sharpen smoke could not find the reader Vue instance."
  Assert-Smoke ($canvasSharpen.tagName -eq "canvas") "Canvas sharpen did not render a canvas page."
  Assert-Smoke ($canvasSharpen.canvasWidth -eq 480 -and $canvasSharpen.canvasHeight -eq 720) "Canvas sharpen dimensions were $($canvasSharpen.canvasWidth)x$($canvasSharpen.canvasHeight), not 480x720."
  Assert-Smoke ($canvasSharpen.canvasSrc -match "/api/v1/books/book-fixed/pages/0") "Canvas sharpen did not use the fixed-page image source."
  Assert-Smoke ($canvasSharpen.pixelReadable) "Canvas sharpen pixel read failed."
  Assert-Smoke (-not $canvasSharpen.horizontalOverflow) "Canvas sharpen reader has horizontal overflow."
  $results += $canvasSharpen

  Wait-Reader -Url "$frontendUrl/read-v2/book-canvas-big"
  $canvasFallback = Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  const findReaderVm = () => {
    const nodes = Array.from(document.querySelectorAll('*'));
    for (const node of nodes) {
      let vm = node.__vue__;
      while (vm) {
        if (vm.`$options?.name === 'ShelfViewReaderShell') return vm;
        vm = vm.`$parent;
      }
    }
    return null;
  };
  const waitFor = async predicate => {
    const deadline = Date.now() + 8000;
    while (Date.now() < deadline) {
      const value = predicate();
      if (value) return value;
      await wait(100);
    }
    return predicate();
  };
  const vm = findReaderVm();
  if (!vm) return {name: 'desktop canvas over-budget fallback', vmFound: false};
  vm.setImageFilterPreset('CANVAS_SHARPEN');
  await vm.`$nextTick();
  const page = await waitFor(() => {
    const candidate = document.querySelector('[data-testid="shelfview-reader-page"]');
    return candidate && candidate.getAttribute('data-canvas-fallback') === 'true' && candidate.complete ? candidate : null;
  });
  return {
    name: 'desktop canvas over-budget fallback',
    url: location.href,
    vmFound: true,
    tagName: page?.tagName.toLowerCase() || '',
    pageComplete: page ? page.complete : false,
    canvasFallback: page?.getAttribute('data-canvas-fallback') === 'true',
    pageSrc: page?.getAttribute('src') || '',
    canvasSrc: page?.getAttribute('data-canvas-src') || '',
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($canvasFallback.vmFound) "Canvas fallback smoke could not find the reader Vue instance."
  Assert-Smoke ($canvasFallback.tagName -eq "img" -and $canvasFallback.pageComplete) "Over-budget canvas page did not fall back to a loaded image."
  Assert-Smoke ($canvasFallback.canvasFallback) "Over-budget canvas fallback marker was not present."
  Assert-Smoke ($canvasFallback.pageSrc -match "/api/v1/books/book-canvas-big/pages/0") "Over-budget canvas fallback did not keep the page source."
  Assert-Smoke (-not $canvasFallback.horizontalOverflow) "Over-budget canvas fallback reader has horizontal overflow."
  $results += $canvasFallback

  Wait-Reader -Url "$frontendUrl/read-v2/book-pdf"
  $desktopPdf = Invoke-CdpExpression @"
(() => {
  const page = document.querySelector('[data-testid="shelfview-reader-page"]');
  return {
    name: 'desktop pdf fixed page',
    url: location.href,
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    live: document.querySelector('[data-testid="shelfview-reader-live-status"]')?.innerText.trim() || '',
    pageVisible: !!page,
    pageComplete: page ? page.complete : false,
    pageSrc: page?.getAttribute('src') || '',
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($desktopPdf.counter -eq "1 / 2") "Desktop PDF counter was not 1 / 2."
  Assert-Smoke ($desktopPdf.live -eq "Page 1 of 2") "Desktop PDF live status was not Page 1 of 2."
  Assert-Smoke ($desktopPdf.pageVisible -and $desktopPdf.pageComplete) "Desktop PDF page image was not loaded."
  Assert-Smoke ($desktopPdf.pageSrc -match "/api/v1/books/book-pdf/pages/0") "Desktop PDF page source did not use the PDF page endpoint."
  Assert-Smoke (-not $desktopPdf.horizontalOverflow) "Desktop PDF reader has horizontal overflow."
  $results += $desktopPdf

  Wait-Reader -Url "$frontendUrl/read-v2/book-folder"
  $desktopFolder = Invoke-CdpExpression @"
(() => {
  const page = document.querySelector('[data-testid="shelfview-reader-page"]');
  return {
    name: 'desktop image folder fixed page',
    url: location.href,
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    live: document.querySelector('[data-testid="shelfview-reader-live-status"]')?.innerText.trim() || '',
    pageVisible: !!page,
    pageComplete: page ? page.complete : false,
    pageSrc: page?.getAttribute('src') || '',
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($desktopFolder.counter -eq "1 / 2") "Desktop image-folder counter was not 1 / 2."
  Assert-Smoke ($desktopFolder.live -eq "Page 1 of 2") "Desktop image-folder live status was not Page 1 of 2."
  Assert-Smoke ($desktopFolder.pageVisible -and $desktopFolder.pageComplete) "Desktop image-folder page image was not loaded."
  Assert-Smoke ($desktopFolder.pageSrc -match "/api/shelfview/v1/books/book-folder/image-directory/pages/0") "Desktop image-folder page source did not use the image-directory endpoint."
  Assert-Smoke (-not $desktopFolder.horizontalOverflow) "Desktop image-folder reader has horizontal overflow."
  $results += $desktopFolder

  Set-Viewport -Width 390 -Height 844 -Mobile $true -DesktopUserAgent $desktopUserAgent
  Wait-Reader -Url "$frontendUrl/read-v2/book-text"
  $mobileText = Invoke-CdpExpression @"
(() => ({
  name: 'mobile android text',
  url: location.href,
  counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
  live: document.querySelector('[data-testid="shelfview-reader-live-status"]')?.innerText.trim() || '',
  firstParagraph: document.querySelector('[data-testid="shelfview-reader-text-paragraph"]')?.innerText.trim() || '',
  horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
  viewport: {width: window.innerWidth, height: window.innerHeight}
}))()
"@
  Assert-Smoke ($mobileText.counter -match "^1 / [1-9][0-9]*$") "Mobile text counter was '$($mobileText.counter)', not a first-page counter."
  Assert-Smoke ($mobileText.live -match "^Paragraph 1 of [1-9][0-9]*$") "Mobile text live status was '$($mobileText.live)', not a first-paragraph status."
  Assert-Smoke ($mobileText.firstParagraph -match "TXT first paragraph") "Mobile text first paragraph did not render."
  Assert-Smoke (-not $mobileText.horizontalOverflow) "Mobile text reader has horizontal overflow."
  $results += $mobileText

  [void] (Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  document.querySelector('[data-testid="shelfview-reader-layout-toggle"]')?.click();
  await wait(400);
  return true;
})()
"@)
  $textEscaping = Invoke-CdpExpression @"
(() => {
  const article = document.querySelector('[data-testid="shelfview-reader-text"]');
  return {
    name: 'mobile android text escaping continuous mode',
    paragraphCount: document.querySelectorAll('[data-testid="shelfview-reader-text-paragraph"]').length,
    escapedScriptText: !!article?.innerText.includes('<script>not html</script>'),
    scriptElementInsideText: !!article?.querySelector('script'),
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth
  };
})()
"@
  Assert-Smoke ($textEscaping.paragraphCount -eq 3) "Mobile text continuous mode did not render all 3 paragraphs."
  Assert-Smoke ($textEscaping.escapedScriptText) "Literal script text was not present as text."
  Assert-Smoke (-not $textEscaping.scriptElementInsideText) "A script element was created inside text content."
  Assert-Smoke (-not $textEscaping.horizontalOverflow) "Mobile text continuous mode has horizontal overflow."
  $results += $textEscaping

  Set-Viewport -Width 390 -Height 844 -Mobile $true -DesktopUserAgent $desktopUserAgent
  Wait-Reader -Url "$frontendUrl/read-v2/book-fixed"
  [void] (Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  document.querySelector('[data-testid="shelfview-reader-layout-toggle"]')?.click();
  await wait(200);
  document.querySelector('[data-testid="shelfview-reader-layout-toggle"]')?.click();
  await wait(200);
  document.querySelector('[data-testid="shelfview-reader-next"]')?.click();
  await wait(300);
  return true;
})()
"@)
  Set-Viewport -Width 844 -Height 390 -Mobile $true -DesktopUserAgent $desktopUserAgent
  [void] (Invoke-CdpExpression @"
(async () => {
  window.dispatchEvent(new Event('orientationchange'));
  window.dispatchEvent(new Event('resize'));
  await new Promise(resolve => setTimeout(resolve, 500));
  return true;
})()
"@)
  $orientation = Invoke-CdpExpression @"
(() => {
  const rect = el => {
    if (!el) return null;
    const b = el.getBoundingClientRect();
    return {top: b.top, bottom: b.bottom, height: b.height};
  };
  const stage = document.querySelector('[data-testid="shelfview-reader-stage"]');
  const current = document.querySelector('[data-page-index="1"]');
  const sr = rect(stage);
  const pr = rect(current);
  return {
    name: 'android portrait to landscape current page restore',
    url: location.href,
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    currentPagePresent: !!current,
    currentPageNearViewport: !!(sr && pr && pr.bottom > sr.top && pr.top < sr.bottom),
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    stage: sr,
    currentPage: pr,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($orientation.counter -eq "2 / 3") "Orientation restore did not stay on page 2."
  Assert-Smoke ($orientation.currentPagePresent -and $orientation.currentPageNearViewport) "Current page was not kept in view after orientation change."
  Assert-Smoke (-not $orientation.horizontalOverflow) "Orientation test has horizontal overflow."
  $results += $orientation

  Set-Viewport -Width 1000 -Height 700 -Mobile $false -DesktopUserAgent $desktopUserAgent
  Wait-Reader -Url "$frontendUrl/read-v2/book-mixed"
  $mixedInitial = Invoke-CdpExpression "({text: document.body.innerText, hasText: !!document.querySelector('[data-testid=shelfview-reader-text]')})"
  [void] (Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  document.querySelector('[data-testid="shelfview-reader-next"]')?.click();
  await wait(250);
  document.querySelector('[data-testid="shelfview-reader-next"]')?.click();
  await wait(500);
  return true;
})()
"@)
  $mixedAfter = Invoke-CdpExpression @"
(() => {
  const page = document.querySelector('[data-testid="shelfview-reader-page"]');
  return {
    name: 'desktop mixed zip text to jpg',
    url: location.href,
    pageVisibleAfterNext: !!page,
    pageSrc: page?.getAttribute('src') || '',
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  $mixedAfter | Add-Member -NotePropertyName initialTextWasVisible -NotePropertyValue ($mixedInitial.hasText -and $mixedInitial.text.Contains("ZIP TXT opening paragraph."))
  Assert-Smoke ($mixedAfter.initialTextWasVisible) "Mixed ZIP text item did not render first."
  Assert-Smoke ($mixedAfter.pageVisibleAfterNext) "Mixed ZIP image item did not render after navigation."
  Assert-Smoke ($mixedAfter.pageSrc -match "/api/shelfview/v1/books/book-mixed/mixed/jpg/pages/0") "Mixed ZIP page source did not use the mixed JPG endpoint."
  Assert-Smoke (-not $mixedAfter.horizontalOverflow) "Mixed ZIP reader has horizontal overflow."
  $results += $mixedAfter

  [void] (Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  document.querySelector('[data-testid="shelfview-reader-next"]')?.click();
  await wait(500);
  return true;
})()
"@)
  $mixedPdf = Invoke-CdpExpression @"
(() => {
  const page = document.querySelector('[data-testid="shelfview-reader-page"]');
  return {
    name: 'desktop mixed zip pdf item',
    url: location.href,
    pageVisibleAfterNext: !!page,
    pageComplete: page ? page.complete : false,
    pageSrc: page?.getAttribute('src') || '',
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($mixedPdf.pageVisibleAfterNext -and $mixedPdf.pageComplete) "Mixed ZIP PDF item did not render after navigation."
  Assert-Smoke ($mixedPdf.pageSrc -match "/api/shelfview/v1/books/book-mixed/mixed/pdf/pages/0") "Mixed ZIP page source did not use the mixed PDF endpoint."
  Assert-Smoke ($mixedPdf.counter -eq "1 / 2") "Mixed ZIP PDF counter was not 1 / 2."
  Assert-Smoke (-not $mixedPdf.horizontalOverflow) "Mixed ZIP PDF reader has horizontal overflow."
  $results += $mixedPdf

  [void] (Invoke-CdpExpression @"
(async () => {
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  document.querySelector('[data-testid="shelfview-reader-next"]')?.click();
  await wait(200);
  document.querySelector('[data-testid="shelfview-reader-next"]')?.click();
  await wait(500);
  return true;
})()
"@)
  $mixedGif = Invoke-CdpExpression @"
(() => {
  const page = document.querySelector('[data-testid="shelfview-reader-page"]');
  return {
    name: 'desktop mixed zip gif item',
    url: location.href,
    pageVisibleAfterNext: !!page,
    pageComplete: page ? page.complete : false,
    pageSrc: page?.getAttribute('src') || '',
    counter: document.querySelector('[data-testid="shelfview-reader-page-counter"]')?.innerText.trim() || '',
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    viewport: {width: window.innerWidth, height: window.innerHeight}
  };
})()
"@
  Assert-Smoke ($mixedGif.pageVisibleAfterNext -and $mixedGif.pageComplete) "Mixed ZIP GIF item did not render after navigation."
  Assert-Smoke ($mixedGif.pageSrc -match "/api/shelfview/v1/books/book-mixed/mixed/gif/pages/0") "Mixed ZIP page source did not use the mixed GIF endpoint."
  Assert-Smoke ($mixedGif.counter -eq "1 / 1") "Mixed ZIP GIF counter was not 1 / 1."
  Assert-Smoke (-not $mixedGif.horizontalOverflow) "Mixed ZIP GIF reader has horizontal overflow."
  $results += $mixedGif

  [PSCustomObject] @{
    browserLabel = $BrowserLabel
    browserExecutable = $script:browserExecutable
    chromeUserAgent = $desktopUserAgent
    bindHost = $BindHost
    publicHost = $PublicHost
    frontend = $frontendUrl
    mockApi = $mockApiUrl
    results = $results
  } | ConvertTo-Json -Depth 20

  Write-Host "ShelfView $BrowserLabel smoke validation passed."
} finally {
  if ($script:ws) {
    $script:ws.Dispose()
  }

  if (-not $KeepServers) {
    for ($i = $startedProcesses.Count - 1; $i -ge 0; $i--) {
      $process = $startedProcesses[$i]
      if ($process -and -not $process.HasExited) {
        Stop-ProcessTree -ProcessId $process.Id
      }
    }
  }
}
