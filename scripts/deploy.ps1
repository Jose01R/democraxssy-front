# scripts/deploy.ps1
#
# Publica el build del frontend en la VM de OCI.
#   1. Verifica que exista dist\index.html (correr antes: npm run build).
#   2. Sube dist\ por scp a una carpeta temporal de la VM.
#   3. La publica en /var/www/democraxssy con rsync --delete, deja root como
#      dueño, normaliza permisos (carpetas 755, archivos 644) y corrige las
#      etiquetas SELinux (restorecon). Sin restorecon, Nginx responde 403
#      porque SELinux no le deja leer los archivos.
#
# El archivo se guarda en UTF-8 con BOM para que PowerShell 5 muestre bien
# las tildes.
#
# Uso (desde la raíz del repo):
#   powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1
#   powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -Key "C:\ruta\a\webserver.key"
#
# La llave privada NUNCA va en el repo. Por defecto se busca en la carpeta
# que contiene al repo (..\webserver.key respecto a la raíz del proyecto).

param(
    [string]$Dist = (Join-Path $PSScriptRoot "..\dist"),
    [string]$Ip   = "129.80.16.61",
    [string]$Key  = (Join-Path $PSScriptRoot "..\..\webserver.key")
)

$ErrorActionPreference = "Stop"
$Remote = "opc@$Ip"

if (-not (Test-Path (Join-Path $Dist "index.html"))) {
    throw "No se encontró $Dist\index.html. Corra 'npm run build' primero."
}
if (-not (Test-Path $Key)) {
    throw "No se encontró la llave SSH en $Key. Pásela con -Key."
}

Write-Host "1/3 Limpiando carpeta temporal en la VM..."
ssh -i $Key $Remote "chmod -R u+w ~/deploy 2>/dev/null; rm -rf ~/deploy"
if ($LASTEXITCODE -ne 0) { throw "Falló la conexión SSH." }

Write-Host "2/3 Subiendo $Dist..."
scp -i $Key -r $Dist "${Remote}:~/deploy"
if ($LASTEXITCODE -ne 0) { throw "Falló la copia con scp." }

Write-Host "3/3 Publicando y corrigiendo etiquetas SELinux..."
ssh -i $Key $Remote "sudo rsync -a --delete --chmod=D755,F644 ~/deploy/ /var/www/democraxssy/ && sudo chown -R root:root /var/www/democraxssy && sudo restorecon -R /var/www/democraxssy && chmod -R u+w ~/deploy && rm -rf ~/deploy"
if ($LASTEXITCODE -ne 0) { throw "Falló la publicación en la VM." }

Write-Host "Listo: https://democraxssy-if6000.duckdns.org/"
