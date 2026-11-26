# Baja, levanta o consulta el backend en la VM, sin tocar la infraestructura.
# Uso (desde infra\):
#   .\servicio.ps1 estado
#   .\servicio.ps1 detener
#   .\servicio.ps1 iniciar
#   .\servicio.ps1 reiniciar
#
# Despues de cada accion verifica desde Internet HEAD /api/salud por HTTP y por HTTPS.
# Con el servicio detenido, Nginx sigue arriba y responde 502 (no hay backend detras).
# Ojo: las partidas viven en memoria; detener o reiniciar las borra.
# La JVM termina con codigo 143 al recibir SIGTERM y systemd lo marca "failed";
# reset-failed lo deja como "inactive", que es lo que realmente es.

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("estado", "detener", "iniciar", "reiniciar")]
    [string] $Accion,
    [string] $Servidor = "157.151.151.205",
    [string] $Dominio  = "api-democraxssy-if6000.duckdns.org",
    [string] $Llave    = ""
)

$ErrorActionPreference = "Stop"
if (-not $Llave) { $Llave = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "ssh-keys\democraxssy-back" }
if (-not (Test-Path $Llave)) { throw "No existe la llave SSH: $Llave" }
$opts = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=20", "-i", $Llave)
$destino = "opc@$Servidor"

$comun = @'
S=democraxssy-back
estado() {
  echo "Servicio:  $(systemctl is-active $S)"
  if systemctl is-active --quiet $S; then
    echo "Desde:     $(systemctl show $S -p ActiveEnterTimestamp --value)"
    echo "Memoria:   $(( $(systemctl show $S -p MemoryCurrent --value) / 1048576 )) MB de 500 MB"
  fi
  echo "Nginx:     $(systemctl is-active nginx)"
}
esperar() {
  for i in $(seq 1 90); do
    c=$(curl -s -o /dev/null -I -w '%{http_code}' --max-time 2 http://127.0.0.1:8080/api/salud || true)
    if [ "$c" = "200" ]; then echo "Backend arriba: HEAD /api/salud -> 200 (tras $i s)"; return 0; fi
    sleep 1
  done
  echo "El backend no respondio en 90 s. Ultimas lineas del log:"; sudo journalctl -u $S -n 20 --no-pager; return 1
}
'@

$acciones = @{
    "estado"    = "estado"
    "detener"   = "sudo systemctl stop `$S; sudo systemctl reset-failed `$S 2>/dev/null; echo 'Backend detenido.'; estado"
    "iniciar"   = "sudo systemctl start `$S && esperar && estado"
    "reiniciar" = "sudo systemctl restart `$S && esperar && estado"
}

# Solo ASCII y sin \r para bash; PowerShell 5.1 agrega \r\n al final: el "#" lo deja como comentario.
$script = ($comun + "`n" + $acciones[$Accion]) -replace "`r", ""
($script + "`n#") | ssh @opts $destino "bash -s"
$ok = $LASTEXITCODE

Write-Host "Desde Internet:" -ForegroundColor Cyan
foreach ($esquema in "http", "https") {
    $url = "${esquema}://$Dominio/api/salud"
    $code = curl.exe -s -o NUL -I -w "%{http_code}" --max-time 10 $url
    Write-Host ("  {0,-55} {1}" -f $url, $code)
}
if ($ok -ne 0) { throw "La accion '$Accion' fallo (ver arriba)" }
