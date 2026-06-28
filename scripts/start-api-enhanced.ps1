$node = "C:\Users\lllk\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
$adb = "C:\Users\lllk\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$serviceDir = "D:\SiListen\.services\api-enhanced"
$logDir = "D:\SiListen\.services\logs"

if (!(Test-Path $node)) {
    throw "Node runtime not found: $node"
}

if (!(Test-Path $serviceDir)) {
    throw "api-enhanced service directory not found: $serviceDir"
}

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

$env:PORT = "3000"
$env:HOST = "0.0.0.0"
$env:CORS_ALLOW_ORIGIN = "*"
$env:ENABLE_GENERAL_UNBLOCK = "true"
$env:ENABLE_FLAC = "true"

$outLog = Join-Path $logDir "api-enhanced.out.log"
$errLog = Join-Path $logDir "api-enhanced.err.log"

Start-Process `
    -FilePath $node `
    -ArgumentList "app.js" `
    -WorkingDirectory $serviceDir `
    -WindowStyle Hidden `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog

if (Test-Path $adb) {
    $serials = & $adb devices | Select-String "`tdevice$" | ForEach-Object {
        ($_ -split "`t")[0].Trim()
    }
    foreach ($serial in $serials) {
        & $adb -s $serial reverse tcp:3000 tcp:3000 | Out-Null
    }
}

$lanIp = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object {
        $_.IPAddress -notlike "127.*" -and
        $_.IPAddress -notlike "169.254.*" -and
        $_.InterfaceAlias -notmatch "vEthernet|Hyper-V|VMware|Virtual|Loopback"
    } |
    Sort-Object SkipAsSource, InterfaceMetric |
    Select-Object -First 1 -ExpandProperty IPAddress

Write-Host "api-enhanced started on http://0.0.0.0:3000"
if ($lanIp) {
    Write-Host "LAN base URL: http://$lanIp`:3000"
}
if (Test-Path $adb) {
    Write-Host "ADB reverse: tcp:3000 -> tcp:3000"
}
Write-Host "Logs: $outLog"
