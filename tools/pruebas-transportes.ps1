# Corre tools/pruebas-contrato.mjs contra el servidor del profesor y el nuestro,
# por HTTPS, por HTTP y por HTTP a traves del proxy del frontend.
# Guarda cada salida en infra/evidencia/07-pruebas/ y muestra un resumen.
#
#   powershell -ExecutionPolicy Bypass -File tools\pruebas-transportes.ps1

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$script = Join-Path $raiz "tools\pruebas-contrato.mjs"
$salida = Join-Path $raiz "infra\evidencia\07-pruebas"
New-Item -ItemType Directory -Force $salida | Out-Null

$api = "api-democraxssy-if6000.duckdns.org"
$front = "democraxssy-if6000.duckdns.org"
$prof = "democraxssy.akamai.meseguercr.com"

$destinos = [ordered]@{
    "profesor-https"     = "https://$prof"
    "profesor-http"      = "http://$prof"
    "nuestro-https"      = "https://$api"
    "nuestro-http"       = "http://$api"
    "nuestro-http-proxy" = "https://$front/api/http/$api"
}

$utf8 = New-Object System.Text.UTF8Encoding $false
$resumen = @()
foreach ($nombre in $destinos.Keys) {
    $url = $destinos[$nombre]
    Write-Host "Probando $nombre ($url)..."
    $texto = (& node $script $url 2>&1 | Out-String)
    [System.IO.File]::WriteAllText((Join-Path $salida "$nombre.txt"), $texto, $utf8)
    $m = [regex]::Match($texto, "(\d+)/(\d+) casos correctos")
    $resultado = if ($m.Success) { $m.Value } else { "sin resultado" }
    $resumen += [pscustomobject]@{ Destino = $nombre; URL = $url; Resultado = $resultado }
}

$fecha = Get-Date -Format "yyyy-MM-dd HH:mm"
$lineas = @("Fecha: $fecha", "", "| Destino | URL | Resultado |", "|---|---|---|")
$lineas += $resumen | ForEach-Object { "| $($_.Destino) | ``$($_.URL)`` | $($_.Resultado) |" }
[System.IO.File]::WriteAllText((Join-Path $salida "resumen.md"), ($lineas -join "`n") + "`n", $utf8)

$resumen | Format-Table -AutoSize
Write-Host "Salidas en infra\evidencia\07-pruebas\"
