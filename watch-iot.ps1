# watch-iot.ps1 — jednostavan live dashboard za IoT pipeline
# Pokretanje:  powershell -ExecutionPolicy Bypass -File watch-iot.ps1

$devices = @(
    "b8:27:eb:bf:9d:51",
    "00:0f:00:70:91:0a",
    "1c:bf:ce:15:ec:4d"
)
$monitoringBase = "http://localhost:8082/monitoring"
$alertBase      = "http://localhost:8083/alert"
$refreshSeconds = 2

# Format kolona (isti za zaglavlje i redove da bi se poklapale).
# Obe tabele dele istu širinu prve dve kolone (Device, pa Temp/Count) -> uredna mreža.
$rowFmt   = "{0,-20} {1,-10} {2,-12} {3,-12} {4,-8}"
$alertFmt = "{0,-20} {1,-10} {2}"

function Get-Json($url) {
    try { return Invoke-RestMethod -Uri $url -TimeoutSec 3 }
    catch { return $null }
}

# Zaokruži broj na N decimala; prazna vrednost -> "-". Tačka kao decimalni separator.
function Fmt($val, $decimals) {
    if ($null -eq $val -or $val -eq "") { return "-" }
    try {
        $n = [math]::Round([double]$val, $decimals)
        return $n.ToString("0.####", [System.Globalization.CultureInfo]::InvariantCulture)
    } catch {
        return [string]$val
    }
}

while ($true) {
    Clear-Host
    Write-Host "=== IoT LIVE DASHBOARD ===  ($(Get-Date -Format 'HH:mm:ss'))   [Ctrl+C to exit]" -ForegroundColor Cyan
    Write-Host ""

    # ---- MONITORING (poslednje stanje uredjaja) ----
    Write-Host "MONITORING - latest state" -ForegroundColor White
    $rowFmt -f "Device","Temp","CO","Humidity","Breach" | Write-Host -ForegroundColor DarkGray
    foreach ($d in $devices) {
        $s = Get-Json "$monitoringBase/$d/state"
        if ($null -eq $s) {
            "{0,-20} {1}" -f $d, "(service unavailable)" | Write-Host -ForegroundColor DarkYellow
        }
        elseif ($s.status -eq "NO_DATA") {
            "{0,-20} {1}" -f $d, "NO_DATA" | Write-Host -ForegroundColor DarkGray
        }
        else {
            $breach = [bool]$s.breach
            $color  = if ($breach) { "Red" } else { "Green" }
            $rowFmt -f `
                $d, (Fmt $s.temperature 1), (Fmt $s.co 4), (Fmt $s.humidity 1), $s.breach |
                Write-Host -ForegroundColor $color
        }
    }

    Write-Host ""
    # ---- ALERTS (broj + poslednji) ----
    Write-Host "ALERTS - active alarms" -ForegroundColor White
    $alertFmt -f "Device","Count","Last Alert" | Write-Host -ForegroundColor DarkGray
    foreach ($d in $devices) {
        $a = Get-Json "$alertBase/$d"
        if ($null -eq $a) {
            "{0,-20} {1}" -f $d, "(service unavailable)" | Write-Host -ForegroundColor DarkYellow
        }
        else {
            $count = @($a).Count
            if ($count -eq 0) {
                $alertFmt -f $d, 0, "" | Write-Host -ForegroundColor DarkGray
            }
            else {
                $last = @($a)[-1].createdAt
                $alertFmt -f $d, $count, $last | Write-Host -ForegroundColor Yellow
            }
        }
    }

    Start-Sleep -Seconds $refreshSeconds
}
