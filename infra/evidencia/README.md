# Evidencia — infraestructura del backend

Respaldo para `DECISIONES.md` y la defensa. Requisitos del enunciado que cubre: desplegado en Internet con HTTPS, backend con HTTP y HTTPS, tecnología DevOps vista en clase (Terraform, Lab #5), uso racional de recursos.

| Archivo | Qué demuestra |
|---|---|
| `01-container-instances-evaluacion.md` | Se evaluó Container Instances (Lab #6): límites, formas, prueba real y por qué se descartó |
| `02-terraform-plan.txt` | El plan de Terraform: 2 recursos (VM Always Free + IP reservada), sin tocar nada existente |
| `03-terraform-apply.txt` | La creación y la recreación con Terraform; la IP reservada se conserva |
| `04-cloud-init-diagnostico.md` | Un problema real (dnf saturaba la RAM), cómo se diagnosticó y cómo se corrigió en el código |
| `05-estado-actual.txt` | Estado verificado: `terraform plan` sin cambios, HTTP y HTTPS, certificado, seguridad, memoria, puertos |
| `06-despliegue-y-rollback.txt` | `deploy.ps1` funcionando y el rollback automático ante un `.jar` roto |
| `07-pruebas-http-https.md` y `07-pruebas/` | Pruebas del contrato contra el servidor del profesor y el nuestro, por HTTPS, HTTP y HTTP vía el proxy del frontend |
| `08-memoria.md` | Memoria con 100 partidas llenas abiertas (2200 jugadores): el servicio pasó de 177 a 187 MB, con tope de 500. Lo genera `tools/prueba-memoria.ps1` |
| `capturas/` | Capturas de la consola de OCI, DuckDNS y el navegador (ver abajo) |

## Datos clave

| Dato | Valor |
|---|---|
| API | `https://api-democraxssy-if6000.duckdns.org` y `http://api-democraxssy-if6000.duckdns.org` |
| VM | `democraxssy-back-e2`, `VM.Standard.E2.1.Micro` (Always Free), Oracle Linux 9.8, AD-1 |
| IP pública | `157.151.151.205` (reservada) |
| Certificado | Let's Encrypt, vence el 19 feb 2027, renovación automática (cron, simulacro OK) |
| Backend | Solo en `127.0.0.1:8080`; Nginx lo expone en 80 y 443 |
| Seguridad | SELinux enforcing, firewalld (ssh, http, https), SSH solo con llave y sin root |
| Costo | $0 (Always Free); el crédito de $300 queda intacto |

## Capturas (`capturas/`)

1. **OCI → Compute → Instances:** las dos VM (`democraxssy-web-e2` y `democraxssy-back-e2`) en `Running`.
2. **OCI → la VM del backend → Metrics:** CPU y memoria (para "uso racional de recursos").
3. **OCI → Networking → Reserved public IPs:** `ip-democraxssy-back`.
4. **OCI → Billing → Upgrade and Manage Payment:** plan Free Tier y el crédito de $300.
5. **DuckDNS:** el subdominio con su IP. **Tapar el token.**
6. **Navegador:** `https://api-democraxssy-if6000.duckdns.org/x` con el candado (y el detalle del certificado).
7. **VS Code:** la terminal con `terraform plan` diciendo `No changes`.
