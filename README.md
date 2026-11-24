# Cards for democraXSSy

Proyecto del curso **IF-6000 · Redes en los Negocios** (UCR, II-2026). Juego de cartas con temática XSS: frontend, backend e infraestructura en un solo repositorio.

> Grupo: Joel · Paolo · José Andrés

## Estructura

```
openapi.yaml   Contrato de la API (spec del profesor). Fuente de verdad para frontend y backend.
frontend/      React + TypeScript + Vite. Ver frontend/README.md
backend/       Spring Boot (Java 21). Implementa openapi.yaml
infra/         Terraform (OCI), cloud-init y despliegue del backend
docs/          Estado del proyecto, pendientes y decisiones
```

## Reglas del repo

- **`openapi.yaml` es el contrato.** Si cambia, el mismo PR actualiza el frontend (`npx orval` desde `frontend/`) y el backend.
- Todo entra a `main` **por PR revisado** por otro compañero.
- Sin secretos en el repo: llaves SSH y de API, `terraform.tfvars`, `*.tfstate`, tokens.

## Sitios

- Frontend: https://democraxssy-if6000.duckdns.org/
- API (nuestro backend): https://api-democraxssy-if6000.duckdns.org y http://api-democraxssy-if6000.duckdns.org
- API del profesor: https://democraxssy.akamai.meseguercr.com
