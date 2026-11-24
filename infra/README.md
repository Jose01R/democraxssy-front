# democraxssy-infra

Infraestructura del backend de Cards for democraXSSy con Terraform en OCI. Sigue la estructura del **Lab #5** (`provider.tf`, `versions.tf`, `availability-domains.tf`, `compute.tf`, `outputs.tf`) y se ejecuta desde **VS Code en Windows**, como en clase.

## Qué crea y qué reutiliza

| Archivo | Qué hace |
|---|---|
| `versions.tf` | Versión del provider `oracle/oci` (igual al Lab #5) |
| `provider.tf` | Autenticación con API key (Lab #5, tutorial 1) |
| `availability-domains.tf` | Lee los AD del tenancy (data source) |
| `compute.tf` | VM `democraxssy-back-e2`: `VM.Standard.E2.1.Micro` (Always Free), Oracle Linux 9 |
| `public-ip.tf` | IP pública **reservada** `ip-democraxssy-back` |
| `cloud-init.yaml` | Lo que se instala al arrancar la VM (ver abajo) |
| `variables.tf` / `terraform.tfvars` | Valores de la cuenta y del proyecto |
| `outputs.tf` | IP pública, IP privada, estado, comando SSH |

**Reutiliza sin tocar:** compartimento `IF6000-26-GUI`, VCN `if6000`, subred pública `10.0.8.0/24` y su Security List (ya permite TCP 22, 80 y 443). A diferencia del tutorial 5 del Lab, **no se crea una red nueva**: la del frontend ya existe y funciona.

**cloud-init instala:** swap de 2 GB, Nginx, Java 21, Certbot, usuario `democraxssy` sin privilegios, servicio `democraxssy-back` (JVM con 256 MB de heap y tope de 500 MB), firewalld, SELinux en *enforcing*, SSH solo con llave y sin root.

**Diseño de Nginx:** Spring Boot escucha solo en `127.0.0.1:8080`. Nginx lo expone por **HTTP (80) y HTTPS (443)** en el subdominio de la API, sin redirigir y **sin HSTS** (para poder demostrar HTTP plano). No agrega cabeceras al reenviar (regla del 409). El límite de 4 KB y el 413 los aplica el backend; Nginx deja pasar hasta 8 KB.

## Preparación (una sola vez)

### 1. Instalar Terraform y la extensión de VS Code

En PowerShell:

```powershell
winget install Hashicorp.Terraform
```

Cerrá y abrí la terminal, y comprobá con `terraform -v`. En VS Code instalá la extensión **HashiCorp Terraform**.

### 2. Crear la API key de OCI (Lab #5, tutorial 1)

1. Consola de OCI → **Profile** (arriba a la derecha) → **User settings** → **API keys** → **Add API key**.
2. Elegí **Generate API key pair** → **Download private key** → **Add**.
3. Guardá la llave en `C:\Users\joell\.oci\oci_api_key.pem`. **No** la pongas en OneDrive ni en el repo.
4. Copiá el **Configuration file preview** (user, fingerprint, tenancy, region): esos valores van en `terraform.tfvars`.

Como sos administrador de tu tenancy, no hacen falta las políticas del tutorial.

### 3. Llave SSH de la VM

Como en el Lab #5 y en el esqueleto del profesor, una llave propia para el backend, dentro de la carpeta:

```powershell
mkdir ssh-keys
ssh-keygen -t rsa -b 2048 -C democraxssy-back -f ssh-keys/democraxssy-back
```

Cuando pida *passphrase*, presioná Enter dos veces (sin contraseña, como en el Lab #5).

`ssh-keys/democraxssy-back` es la privada (queda fuera de git por el `.gitignore`); `ssh-keys/democraxssy-back.pub` es la pública que Terraform pone en la VM.

> **Alternativa:** reutilizar la llave del frontend. Sacá su pública con `ssh-keygen -y -f <ruta>\webserver.key > ssh-keys/webserver.pub` y poné esa ruta en `ssh_public_key_path`. Funciona igual, pero si esa llave se filtra, quedan expuestas las dos VM.

> La llave SSH **no se puede cambiar** después de crear la VM (Lab #5, tutorial 3).

### 4. Variables

Copiá `terraform.tfvars.example` como `terraform.tfvars` y completá los valores de la API key.

Para `ad_index`: tiene que ser el AD de la VM del frontend (E2.1.Micro solo es Always Free en uno). En Cloud Shell:

```bash
oci compute instance list -c ocid1.compartment.oc1..aaaaaaaav4upi7w6loxn2u4rmoekudmjmeshevuqir34rpyqwr5vzjajo27a \
  --query "data[?\"display-name\"=='democraxssy-web-e2'].\"availability-domain\" | [0]" --raw-output
```

`AD-1` → `0`, `AD-2` → `1`, `AD-3` → `2`.

## Crear la infraestructura

En la terminal de VS Code, dentro de la carpeta:

```powershell
terraform init
terraform validate
terraform plan -out .plan
terraform apply .plan
```

El `plan` debe decir `Plan: 2 to add, 0 to change, 0 to destroy` (la VM y la IP reservada). Guardá la salida de `plan` y `apply` como evidencia. Al final, `terraform output` muestra la IP pública.

## Después del apply

1. **DNS:** en duckdns.org, poné `public-ip-for-backend` en el subdominio de la API.
2. **Esperar a cloud-init** (unos minutos en la E2.1.Micro):
   ```powershell
   ssh -i ssh-keys/democraxssy-back opc@<public-ip-for-backend>
   sudo cloud-init status --wait
   ```
   Tiene que terminar en `status: done`. Si dice `error`: `sudo tail -n 50 /var/log/cloud-init-output.log`.
3. **Certificado HTTPS** (con el DNS ya apuntando):
   ```bash
   sudo certbot --nginx -d api-democraxssy-if6000.duckdns.org --no-redirect --agree-tos -m <tu-correo>
   ```
4. **Comprobar:**
   ```powershell
   curl.exe -sI http://api-democraxssy-if6000.duckdns.org/api/salud
   curl.exe -sI https://api-democraxssy-if6000.duckdns.org/api/salud
   ```
   Sin `.jar` desplegado, las dos responden **502**: Nginx funciona y el backend todavía no existe. Es lo esperado.

## Desplegar el backend

Con el `.jar` de Spring Boot que entrega el backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1 -Jar <ruta>\democraxssy-back.jar
```

El script:
1. Sube el `.jar` a la VM por `scp` con la llave `ssh-keys/democraxssy-back`.
2. Guarda la versión anterior como `democraxssy-back.jar.prev`, instala la nueva y reinicia el servicio.
3. Espera hasta 120 s a que `HEAD /api/salud` responda 2xx. Si no responde, **vuelve a la versión anterior** (rollback) y muestra el log.
4. Verifica la API desde Internet por HTTP y HTTPS.

En `prueba/` hay un backend mínimo (`backend-prueba.jar`, fuente en `Stub.java`, sin Spring) que responde `200` en `/api/salud`. Sirve para probar la infraestructura sin el backend real.

## Notas

- **Estado:** `terraform.tfstate` queda en la carpeta (como en el esqueleto del profesor). No lo borren; sin él Terraform no sabe qué creó.
- Cambiar `cloud-init.yaml` **recrea la VM** en el siguiente `apply` (la IP reservada se conserva). Una imagen nueva de Oracle Linux no la recrea.
- `terraform destroy` elimina la VM y libera la IP reservada.
