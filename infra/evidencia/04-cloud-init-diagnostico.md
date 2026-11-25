# Diagnóstico y corrección de cloud-init (21 nov 2026)

## Síntoma

Después del primer `apply`, la VM quedó `RUNNING` pero no respondía:

```
ssh : Connection timed out during banner exchange      (8 intentos en 7 minutos)
puerto 22: abierto
puerto 80: sin respuesta
puerto 443: sin respuesta
```

## Diagnóstico (consola serial de la VM, desde Cloud Shell)

```
$ oci compute console-history capture --instance-id $INST
$ oci compute console-history get-content ... | tail -n 60
[   81.616080] cloud-init[5637]: Setting up swapspace version 1, size = 2 GiB
[  162.578990] cloud-init[5637]: Ksplice for Oracle Linux 9 (x86_64)            13 MB/s |  39 MB
[  193.242596] cloud-init[5637]: Oracle Linux 9 OCI Included Packages (x86_64)  40 MB/s | 190 MB
                (sin más líneas durante ~30 minutos)
```

Y por SSH, una vez que respondió, en la segunda VM:

```
Mem:   total 498   used 402   free 14   available 95
Swap:  total 2545  used 1733
/usr/bin/dnf install -y nginx java-21-openjdk-headless oracle-epel-release-el9   (47 % de la RAM)
```

**Causa:** `dnf` descargaba y procesaba los índices de **todos** los repositorios de la imagen (OCI Included 190 MB, Ksplice, EPEL). Con 1 GB de RAM la VM quedaba usando swap todo el tiempo y ni SSH respondía.

**Ajuste adicional:** `kdump` (volcados del kernel) no se usa en este servidor y puede reservar memoria (`crashkernel`). En la segunda VM `free` mostró solo 498 MB totales; en la tercera ya eran 945 MB antes del reinicio, así que no queda demostrado que kdump fuera la causa. Igual se desactivó, porque no aporta nada aquí.

## Corrección (en `cloud-init.yaml`)

| Cambio | Motivo |
|---|---|
| `dnf --disablerepo='*' --enablerepo=ol9_baseos_latest --enablerepo=ol9_appstream` | Solo los dos repos necesarios (Nginx y Java están ahí) |
| `systemctl disable --now dnf-makecache.timer` | Evita que dnf vuelva a descargar índices en segundo plano |
| Certbot con `pip` en `/opt/certbot` (venv) | Método oficial de Certbot; evita el repositorio EPEL |
| `systemctl disable kdump` + `grubby --remove-args=crashkernel` + reinicio | Desactiva un servicio que no se usa y evita que reserve memoria |
| Quitar `metadata["user_data"]` de `ignore_changes` en `compute.tf` | Que un cambio en cloud-init recree la VM (la IP reservada se conserva) |

## Resultado

```
cloud-init status: done   errors: []
Mem:  total 945  used 490  available 455
Swap: total 2992 used 0
```

Tiempo de cloud-init: unos 10 minutos (antes no terminaba en más de 30).
