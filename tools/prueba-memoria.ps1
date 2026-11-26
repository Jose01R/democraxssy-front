# Prueba de memoria del backend (fase E): abre muchas partidas con tools/carga-partidas.mjs
# y mide por SSH la memoria del servicio (cgroup de systemd, tope MemoryMax=500M) y la de la VM.
# Guarda el resultado en infra/evidencia/08-memoria.md.
#
#   powershell -ExecutionPolicy Bypass -File tools\prueba-memoria.ps1
#   powershell -ExecutionPolicy Bypass -File tools\prueba-memoria.ps1 -Partidas 40 -EsperarMin 15
#
# -EsperarMin: minutos a esperar despues de la carga para ver si la limpieza de partidas inactivas libera memoria.
# Las partidas no se pueden eliminar sin terminarlas (contrato): se van con esa limpieza o al reiniciar el servicio.

param(
    [string] $Url        = "https://api-democraxssy-if6000.duckdns.org",
    [int]    $Partidas   = 20,
    [int]    $Jugadores  = 22,
    [int]    $Ritmo      = 12,
    [int]    $EsperarMin = 0,
    [string] $Servidor   = "157.151.151.205",
    [string] $Llave      = ""
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $Llave) { $Llave = Join-Path $raiz "infra\ssh-keys\democraxssy-back" }
if (-not (Test-Path $Llave)) { throw "No existe la llave SSH: $Llave" }
$carga = Join-Path $raiz "tools\carga-partidas.mjs"
$opts = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=20", "-i", $Llave)
$destino = "opc@$Servidor"

$remoto = @'
S=democraxssy-back
echo "$(systemctl show $S -p MemoryCurrent --value) $(systemctl show $S -p MemoryMax --value) $(free -m | awk 'NR==2{print $3, $2}')"
'@ -replace "`r", ""

function Medir([string] $momento) {
    # PowerShell 5.1 agrega \r\n al final de lo que manda por la tuberia: el "#" final lo deja como comentario.
    $linea = ($remoto + "`n#") | ssh @opts $destino "bash -s"
    if ($LASTEXITCODE -ne 0) { throw "Fallo la medicion por SSH" }
    $v = "$linea".Trim() -split "\s+"
    [pscustomobject]@{
        Momento     = $momento
        ServicioMB  = [math]::Round([double]$v[0] / 1MB)
        TopeMB      = [math]::Round([double]$v[1] / 1MB)
        VmUsadaMB   = [int]$v[2]
        VmTotalMB   = [int]$v[3]
    }
}

$medidas = @()
$medidas += Medir "Antes de la carga"
Write-Host "Antes: servicio $($medidas[0].ServicioMB) MB de $($medidas[0].TopeMB) MB"

Write-Host "Abriendo $Partidas partidas de $Jugadores jugadores a $Ritmo solicitudes/s..."
$trabajo = Start-Job -ScriptBlock {
    param($carga, $url, $p, $j, $r)
    node $carga $url --partidas $p --jugadores $j --ritmo $r 2>$null
} -ArgumentList $carga, $Url, $Partidas, $Jugadores, $Ritmo

$pico = $null
while ($trabajo.State -eq "Running") {
    Start-Sleep -Seconds 10
    $m = Medir "Durante"
    if (-not $pico -or $m.ServicioMB -gt $pico.ServicioMB) { $pico = $m }
    Write-Host "  servicio $($m.ServicioMB) MB, VM $($m.VmUsadaMB)/$($m.VmTotalMB) MB"
}
$resultado = (Receive-Job $trabajo | Out-String).Trim()
Remove-Job $trabajo
Write-Host "Carga: $resultado"

if ($pico) { $pico.Momento = "Pico durante la carga"; $medidas += $pico }
$medidas += Medir "Con las partidas abiertas"

if ($EsperarMin -gt 0) {
    Write-Host "Esperando $EsperarMin min para ver la limpieza de partidas inactivas..."
    Start-Sleep -Seconds ($EsperarMin * 60)
    $medidas += Medir "Tras $EsperarMin min sin actividad"
}

$medidas | Format-Table -AutoSize

$fecha = Get-Date -Format "yyyy-MM-dd HH:mm"
$md = @(
    "# Prueba de memoria del backend",
    "",
    "Fecha: $fecha. Destino: ``$Url``. Carga: $Partidas partidas de $Jugadores jugadores, iniciadas (cada jugador con 7 cartas).",
    "Se repite con ``powershell -ExecutionPolicy Bypass -File tools\prueba-memoria.ps1``.",
    "",
    "| Momento | Servicio (MB) | Tope del servicio (MB) | RAM usada de la VM (MB) |",
    "|---|---:|---:|---:|"
)
$md += $medidas | ForEach-Object { "| $($_.Momento) | $($_.ServicioMB) | $($_.TopeMB) | $($_.VmUsadaMB) de $($_.VmTotalMB) |" }
$md += @("", "Resultado de la carga: ``$resultado``")
$archivo = Join-Path $raiz "infra\evidencia\08-memoria.md"
[System.IO.File]::WriteAllText($archivo, ($md -join "`n") + "`n", (New-Object System.Text.UTF8Encoding $false))
Write-Host "Evidencia en infra\evidencia\08-memoria.md"
