param(
  [string]$K6Path = 'C:\Program Files\k6\k6.exe',
  [string]$OutputDirectory = '',
  [switch]$SkipChecks
)

$ErrorActionPreference = 'Stop'
$adminDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repoDirectory = (Resolve-Path (Join-Path $adminDirectory '..')).Path
$scriptPath = Join-Path $PSScriptRoot 'public-read.k6.js'
$environment = if ($env:TARGET_ENV) { $env:TARGET_ENV.ToLowerInvariant() } else { '' }
$targetUrl = $env:SUPABASE_URL
$publishableKey = if ($env:SUPABASE_PUBLISHABLE_KEY) { $env:SUPABASE_PUBLISHABLE_KEY } else { $env:SUPABASE_ANON_KEY }

# For local runs, reuse the app's public config only when the caller explicitly
# selected TARGET_ENV=local. The loopback-host guard below still rejects a
# hosted Supabase URL from .env.local.
if ($environment -eq 'local' -and (Test-Path -LiteralPath (Join-Path $adminDirectory '.env.local'))) {
  foreach ($line in Get-Content -LiteralPath (Join-Path $adminDirectory '.env.local')) {
    if ($line -match '^\s*(NEXT_PUBLIC_SUPABASE_URL|NEXT_PUBLIC_SUPABASE_ANON_KEY)\s*=\s*(.*)$') {
      $name = $Matches[1]
      $value = $Matches[2].Trim().Trim('"').Trim("'")
      if ($name -eq 'NEXT_PUBLIC_SUPABASE_URL' -and [string]::IsNullOrWhiteSpace($targetUrl)) { $targetUrl = $value }
      if ($name -eq 'NEXT_PUBLIC_SUPABASE_ANON_KEY' -and [string]::IsNullOrWhiteSpace($publishableKey)) { $publishableKey = $value }
    }
  }
}

if (-not (Test-Path -LiteralPath $K6Path -PathType Leaf)) { throw "k6 executable not found: $K6Path" }
if ($environment -notin @('local', 'staging')) { throw 'Set TARGET_ENV to local or staging. The shared hosted project is prohibited.' }
if ([string]::IsNullOrWhiteSpace($targetUrl) -or [string]::IsNullOrWhiteSpace($publishableKey)) {
  throw 'Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY (or SUPABASE_ANON_KEY) in this PowerShell session.'
}
$env:SUPABASE_URL = $targetUrl
$env:SUPABASE_PUBLISHABLE_KEY = $publishableKey
if ($publishableKey -match '^sb_secret_' -or $publishableKey -match 'service_role') {
  throw 'A secret/service-role key is not allowed. Use the public publishable/anon key.'
}
try { $uri = [uri]$targetUrl } catch { throw 'SUPABASE_URL must be a valid absolute URL.' }
if ($environment -eq 'local' -and $uri.Host -notin @('localhost', '127.0.0.1', '::1')) {
  throw 'TARGET_ENV=local requires a localhost or loopback URL.'
}
if ($environment -eq 'staging') {
  if ($uri.Scheme -ne 'https') { throw 'Staging URL must use HTTPS.' }
  $projectRef = $env:STAGING_SUPABASE_PROJECT_REF
  if ([string]::IsNullOrWhiteSpace($projectRef) -or $uri.Host -ne "$projectRef.supabase.co") {
    throw 'For staging, set STAGING_SUPABASE_PROJECT_REF to the approved staging project reference and ensure it matches SUPABASE_URL.'
  }
}
if ($uri.Host -eq 'kjoykvxrnyykdnzektoj.supabase.co') { throw 'The shared Bodla Supabase project cannot be used by this progression runner.' }
if ($publishableKey -match '^eyJ') {
  try {
    $payloadSegment = $publishableKey.Split('.')[1].Replace('-', '+').Replace('_', '/')
    while (($payloadSegment.Length % 4) -ne 0) { $payloadSegment += '=' }
    $payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payloadSegment)) | ConvertFrom-Json
    if ($payload.role -eq 'service_role') { throw 'A service-role JWT is not allowed. Use the public anon key.' }
  } catch { if ($_.Exception.Message -like '*service-role*') { throw }; throw 'The configured JWT could not be safely inspected; use a public anon/publishable key.' }
}

if (-not $OutputDirectory) {
  $OutputDirectory = Join-Path $PSScriptRoot ('results\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

if (-not $SkipChecks) {
  Push-Location $adminDirectory
  try {
    foreach ($check in @(@('npm.cmd', 'run', 'lint'), @('npx.cmd', 'tsc', '--noEmit'), @('npm.cmd', 'run', 'build'))) {
      & $check[0] $check[1..($check.Length - 1)]
      if ($LASTEXITCODE -ne 0) { throw "Preflight check failed: $($check -join ' ')" }
    }
  } finally { Pop-Location }
}

$env:TEST_PROFILE = 'smoke'
$env:MAX_VUS = '5'
$env:HOLD_DURATION = '30s'
& $K6Path run --summary-export (Join-Path $OutputDirectory 'smoke.json') $scriptPath 2>&1 |
  Tee-Object -FilePath (Join-Path $OutputDirectory 'smoke.log')
if ($LASTEXITCODE -ne 0) { throw 'Smoke test failed; progression stopped.' }

$reportRows = [System.Collections.Generic.List[object]]::new()
foreach ($vus in @(1, 10, 25, 50, 100, 250, 500, 750, 1000)) {
  Write-Host "Starting sequential staging step: $vus VUs"
  $env:TEST_PROFILE = 'stage'
  $env:MAX_VUS = [string]$vus
  $env:HOLD_DURATION = '1m'
  $stageName = "stage-$vus"
  $jsonPath = Join-Path $OutputDirectory "$stageName.json"
  $logPath = Join-Path $OutputDirectory "$stageName.log"
  & $K6Path run --summary-export $jsonPath $scriptPath 2>&1 | Tee-Object -FilePath $logPath
  $exitCode = $LASTEXITCODE
  if (Test-Path -LiteralPath $jsonPath) {
    $summary = Get-Content -LiteralPath $jsonPath -Raw | ConvertFrom-Json
    foreach ($endpoint in @('notices', 'events', 'emergency_contacts')) {
      $metric = $summary.metrics."http_req_duration{endpoint:$endpoint}"
      $statusMetrics = $summary.metrics.PSObject.Properties | Where-Object { $_.Name -like "http_status_codes{endpoint:$endpoint,*" }
      $errorMetrics = $summary.metrics.PSObject.Properties | Where-Object { $_.Name -like "http_error_codes{endpoint:$endpoint,*" }
      $reportRows.Add([pscustomobject]@{
        vus = $vus; endpoint = $endpoint; p50_ms = $metric.values.med; p90_ms = $metric.values.'p(90)'
        p95_ms = $metric.values.'p(95)'; p99_ms = $metric.values.'p(99)'
        request_rate = $summary.metrics.http_reqs.values.rate
        failed_rate = $summary.metrics.http_req_failed.values.rate
        completed_iterations = $summary.metrics.iterations.values.count
        dropped_iterations = $summary.metrics.dropped_iterations.values.count
        status_codes = (($statusMetrics | ForEach-Object { "$($_.Name)=$($_.Value.values.count)" }) -join ';')
        error_codes = (($errorMetrics | ForEach-Object { "$($_.Name)=$($_.Value.values.count)" }) -join ';')
        exit_code = $exitCode
      })
    }
    $reportRows | Export-Csv -LiteralPath (Join-Path $OutputDirectory 'progression.csv') -NoTypeInformation
  }
  if ($exitCode -ne 0) { throw "Threshold or runtime failure at $vus VUs; stopped. Inspect $logPath and matching Supabase logs." }
}

Write-Host "All stages passed. Reports: $OutputDirectory"
