# Evaluación de OCI Container Instances (descartada)

Antes de elegir Terraform se comprobó si la cuenta permitía usar Container Instances (Lab #6). Todo se ejecutó en OCI Cloud Shell, región `us-ashburn-1`.

## 1. Servicios de límites con "container" (14 nov 2026)

```
$ oci limits service list --compartment-id $OCI_TENANCY --all \
    --query "data[?contains(name,'container')].name" --output table
| container-engine   |
| container-registry |
```

No existe un servicio de límites propio para Container Instances.

## 2. Formas disponibles para Container Instances

```
$ oci container-instances container-instance list-shapes --compartment-id $OCI_TENANCY
CI.Standard.A1.Flex  (3.0 GHz Ampere Altra, ARM)   1–76 OCPU
CI.Standard.E4.Flex  (2.25 GHz AMD EPYC 7J13, x86) 1–64 OCPU
```

## 3. Límites de Compute de la cuenta (resumen)

| Límite | Valor |
|---|---|
| `standard-e4-core-count` (AD-1, AD-2, AD-3) | **0** → no se puede usar `CI.Standard.E4.Flex` |
| `standard-a1-core-regional-count` | 16 OCPU |
| `standard-a1-memory-regional-count` | 96 GB |

## 4. Tipo de cuenta

- Plan: **Free Tier** (inicio 26 ago 2026).
- Crédito disponible: **$300**, vence el 25 ago 2027.

> Capturas pendientes: *Billing → Upgrade and Manage Payment* y el medidor de crédito.

## 5. Prueba de humo (15 nov 2026, 22:42–22:45)

Se creó una Container Instance `CI.Standard.A1.Flex` (1 OCPU, 1 GB) con `docker.io/library/nginx:latest` en la subred pública `10.0.8.0/24`:

```
$ echo "$CI"
ocid1.computecontainerinstance.oc1.iad.anuwcljtvyootfyaayeyl3cxkuenwcjzsl2wvxqs7mdm5jj6bym3nhuwlxda
CREATING
ACTIVE
$ echo "$IP"; curl -sI "http://$IP" | head -n 3
129.153.238.162
HTTP/1.1 200 OK
Server: nginx/1.31.8
```

Después se eliminó con `oci container-instances container-instance delete --force`.

## Conclusión

Container Instances **funciona** en la cuenta (solo con A1/ARM y pagado con el crédito), pero se descartó porque:

1. **Costo:** consume crédito; la VM `E2.1.Micro` es Always Free.
2. **IP efímera:** cambia al recrear la instancia, y hay que actualizar el DNS en cada despliegue. La VM usa una IP reservada.
3. **HTTPS:** requiere un contenedor extra (Caddy/Nginx) y el certificado se pierde al recrear, con riesgo de chocar con los límites de Let's Encrypt.
4. **Arquitectura:** solo A1 (ARM), así que la imagen de Spring Boot tendría que construirse para `arm64`.

El profesor confirmó (19 nov) que Terraform con una VM nueva y cloud-init es válido como tecnología DevOps.
