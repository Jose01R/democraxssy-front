# Guion: infraestructura y despliegue en caliente del backend (defensa)

Objetivo: mostrar en vivo que la infraestructura del backend está definida como código (Terraform), que coincide con lo desplegado, y que una versión nueva del backend se publica con un comando, verificada por HTTP y HTTPS.

Duración: unos 8 minutos. Lo presenta Joel.

---

## Antes de la presentación (30 min antes)

- [ ] `.jar` final del backend compilado desde `main`: `cd backend`, borrar `target\classes\org` y `target\test-classes` (o `mvnw clean`, si OneDrive lo permite) y `.\mvnw.cmd package`.
- [ ] VS Code abierto en `democraxssy-front`, terminal en `infra\`.
- [ ] Probar que Terraform autentica: `terraform plan` → `No changes`.
- [ ] Llave SSH con permisos correctos (si `deploy.ps1` dice *bad permissions*):
      `icacls ssh-keys\democraxssy-back /inheritance:r /grant:r "$($env:USERNAME):R"`
- [ ] Pestañas abiertas en el navegador:
  1. Frontend: `https://democraxssy-if6000.duckdns.org/`
  2. API: `https://api-democraxssy-if6000.duckdns.org/api/salud`
  3. Consola de OCI → *Compute → Instances* (las dos VM)
  4. Consola de OCI → VM del backend → *Monitoring* (CPU y memoria)
- [ ] `.\servicio.ps1 estado` → `Servicio: active` y `200` por HTTP y HTTPS.
- [ ] Prueba rápida: `node tools/partida-completa.mjs https://api-democraxssy-if6000.duckdns.org` → todo `OK`.

> **Importante:** el backend guarda las partidas en memoria. **Desplegar reinicia el servicio y borra las partidas en curso.** Hacer el despliegue **antes** de la partida de demostración, nunca durante.

---

## En vivo

### 1. La infraestructura es código (1 min)

Mostrar en VS Code:
- `compute.tf`: la VM `VM.Standard.E2.1.Micro` (Always Free) y `cloud-init`.
- `public-ip.tf`: la IP pública **reservada**.
- `cloud-init.yaml`: Nginx, Java 21, Certbot, firewall, SELinux, servicio del backend con memoria limitada.

> "La VM no se configuró a mano: Terraform la crea y cloud-init la deja lista. Si se destruye, `terraform apply` la recrea igual, con la misma IP."

### 2. Lo desplegado coincide con el código (1 min)

```powershell
terraform plan
```
Resultado esperado: **`No changes. Your infrastructure matches the configuration.`**

> "Terraform compara lo que hay en OCI con el código y no encuentra diferencias."

Mostrar la pestaña de *Instances* en OCI: `democraxssy-back-e2` en *Running*, Always Free.

### 3. Bajar y levantar el backend en caliente (1 min)

```powershell
.\servicio.ps1 estado
.\servicio.ps1 detener
```
Salida esperada al detener:
```
Backend detenido.
Servicio:  inactive
Nginx:     active
Desde Internet:
  http://api-democraxssy-if6000.duckdns.org/api/salud     502
  https://api-democraxssy-if6000.duckdns.org/api/salud    502
```
> "Detuvimos solo el proceso Java. Nginx sigue arriba y responde 502 porque no tiene backend detrás; la VM y la red no se tocan."

En el frontend, la verificación de **Nuestro backend (HTTPS)** falla. Después:
```powershell
.\servicio.ps1 iniciar
```
Salida esperada: `Backend arriba: HEAD /api/salud -> 200 (tras ~18 s)` y `200` por HTTP y HTTPS. En el frontend vuelve a salir **Mesa Verificada**.

> "Si el proceso se cae solo, systemd lo reinicia (`Restart=on-failure`); esto es para hacerlo a mano."

### 4. Despliegue en caliente (2 min)

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1 -Jar ..\backend\target\backend-0.0.1-SNAPSHOT.jar
```
Salida esperada:
```
1/3 Subiendo ... a 157.151.151.205...
2/3 Instalando y reiniciando el servicio...
Backend arriba: HEAD /api/salud -> 200 (tras ~18 s)
Memoria del servicio: ~140 MB
3/3 Verificando desde Internet...
  http://api-democraxssy-if6000.duckdns.org/api/salud     200
  https://api-democraxssy-if6000.duckdns.org/api/salud    200
Listo.
```
> "Sube el `.jar`, guarda la versión anterior, reinicia el servicio y espera a que responda. Si no responde en 120 s, vuelve solo a la versión anterior. Al final verifica desde Internet por HTTP y por HTTPS."

### 5. HTTP y HTTPS (2 min)

```powershell
curl.exe -sI http://api-democraxssy-if6000.duckdns.org/api/salud
curl.exe -sI https://api-democraxssy-if6000.duckdns.org/api/salud
```
Los dos `200`, sin `Location` (no redirige) y sin `Strict-Transport-Security` (no fuerza HTTPS).

En el frontend:
1. Botón **Nuestro backend (HTTPS)** → crear partida → en el panel de llamadas se ve la URL `https://api-...` (conexión directa).
2. Botón **Nuestro backend (HTTP vía proxy)** → crear partida → se ve `/api/http/api-...` (el navegador no permite HTTP desde una página HTTPS; el Nginx del frontend hace de puente).

### 6. Recursos (1 min)

Pestaña *Monitoring* de la VM: CPU baja, memoria estable.
> "Una VM de 1 GB Always Free: el backend usa entre 140 y 190 MB, con la JVM limitada a 256 MB y el servicio a 500 MB. Con 2200 jugadores en 100 partidas no pasó de 187 MB. Costo: cero."

---

## Si algo falla

| Problema | Qué hacer |
|---|---|
| `terraform plan` da error de autenticación (401) | Mostrar `evidencia/05-estado-actual.txt` (plan con `No changes`) y la captura `07-terraform-apply.png` |
| `deploy.ps1` falla | El rollback deja la versión anterior funcionando. Mostrar el log que imprime y explicar el rollback (`evidencia/06-despliegue-y-rollback.txt`) |
| *bad permissions* en la llave | Correr el `icacls` de la lista previa |
| La VM no responde | No recrearla en vivo (cloud-init tarda ~10 min). Mostrar la evidencia y explicar `terraform apply -replace` |
| OCI o Internet caído | Evidencia en `infra/evidencia/` y capturas |

---

## Preguntas probables

- **¿Por qué Terraform y no Container Instances?** La cuenta es Free Tier: la VM es Always Free; Container Instances consume crédito, solo permite ARM, la IP es efímera y el HTTPS necesita un contenedor extra. Lo probamos (evidencia 01).
- **¿Por qué otra VM y no la del frontend?** 1 GB no alcanza para los dos, el proxy anti-SSRF del frontend bloquea su propia IP y el dominio del frontend envía HSTS (forzaría HTTPS).
- **¿Cómo soporta HTTP y HTTPS?** Nginx escucha en 80 y 443 con certificado de Let's Encrypt; el 80 no redirige. El backend solo escucha en `127.0.0.1:8080`.
- **¿Qué pasa si se cae la VM?** `terraform apply` la recrea igual; la IP es reservada, así que el DNS no cambia.
- **¿Y si el despliegue sale mal?** Rollback automático a la versión anterior.
- **¿Por qué no hay base de datos?** Las partidas son efímeras; guardarlas en memoria ahorra RAM (SQL Server pide 2 GB). A cambio, un reinicio las borra.
