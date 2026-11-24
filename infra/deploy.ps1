# Despliega el .jar del backend en la VM creada por Terraform.
# Uso:  .\deploy.ps1 -Jar ruta\al\democraxssy-back.jar
#
# Pasos: sube el .jar, guarda la version anterior, reinicia el servicio y espera a que
# HEAD /api/salud responda 2xx. Si no responde, vuelve a la version anterior (rollback).

param(
    [Parameter(Mandatory = $true)] [string] $Jar,
    [string] $Servidor = "157.151.151.205",
    [string] $Dominio  = "api-democraxssy-if6000.duckdns.org",
    [string] $Llave    = ""
)

$ErrorActionPreference = "Stop"
if (-not $Llave) { $Llave = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "ssh-keys\democraxssy-back" }
if (-not (Test-Path $Jar))   { throw "No existe el archivo: $Jar" }
if (-not (Test-Path $Llave)) { throw "No existe la llave SSH: $Llave" }

$opts = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=20", "-i", $Llave)
$destino = "opc@$Servidor"
$tamano = [math]::Round((Get-Item $Jar).Length / 1MB, 1)

Write-Host "1/3 Subiendo $Jar ($tamano MB) a $Servidor..." -ForegroundColor Cyan
scp @opts $Jar "${destino}:/tmp/democraxssy-back.jar.new"
if ($LASTEXITCODE -ne 0) { throw "Fallo scp" }

# Script que corre en la VM (solo ASCII y sin \r para bash)
$remoto = @'
set -euo pipefail
J=/opt/democraxssy/democraxssy-back.jar
sudo install -o root -g democraxssy -m 0640 /tmp/democraxssy-back.jar.new "$J.new"
rm -f /tmp/democraxssy-back.jar.new
if sudo test -f "$J"; then sudo cp -p "$J" "$J.prev"; fi
sudo mv -f "$J.new" "$J"
sudo systemctl restart democraxssy-back
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -I -w '%{http_code}' --max-time 2 http://127.0.0.1:8080/api/salud || true)
  case "$code" in
    2*) echo "Backend arriba: HEAD /api/salud -> $code (tras $((i*2)) s)"
        systemctl show democraxssy-back -p MemoryCurrent --value | awk '{printf "Memoria del servicio: %d MB\n", $1/1048576}'
        exit 0 ;;
  esac
  sleep 2
done
echo "FALLO: el backend no respondio en 120 s. Ultimas lineas del log:"
sudo journalctl -u democraxssy-back -n 30 --no-pager
if sudo test -f "$J.prev"; then
  sudo mv -f "$J.prev" "$J"
  sudo systemctl restart democraxssy-back
  echo "ROLLBACK: se restauro la version anterior"
fi
exit 1
'@ -replace "`r", ""
$remoto += "`n"   # la ultima linea tambien termina en LF

Write-Host "2/3 Instalando y reiniciando el servicio..." -ForegroundColor Cyan
$remoto | ssh @opts $destino "bash -s"
if ($LASTEXITCODE -ne 0) { throw "El despliegue fallo (ver el log arriba)" }

Write-Host "3/3 Verificando desde Internet..." -ForegroundColor Cyan
foreach ($esquema in "http", "https") {
    $url = "${esquema}://$Dominio/api/salud"
    $code = curl.exe -s -o NUL -I -w "%{http_code}" --max-time 10 $url
    Write-Host ("  {0,-55} {1}" -f $url, $code)
}
Write-Host "Listo." -ForegroundColor Green
