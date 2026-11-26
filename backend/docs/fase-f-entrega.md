# Fase F — Empaquetado y entrega del `.jar` a infra

Runbook para cortar el `.jar` ejecutable final del backend, verificarlo en limpio y
entregárselo a Joel para desplegarlo en la VM.

## 1. Requisitos
- **Java 21** (`java -version` debe decir 21).
- Estar en la raíz del backend: `cd backend`.

## 2. Construir el `.jar`
```
./mvnw clean package
```
- **`clean` es obligatorio.** Borra `target/classes`; si quedan clases viejas (p. ej. el
  antiguo `JuegoServicioPendiente`, ya eliminado), Spring puede encontrar **dos beans**
  `JuegoServicio` y el arranque falla.
- Corre toda la suite de pruebas antes de empacar. Debe decir `BUILD SUCCESS`.
- Artefacto: **`target/backend-0.0.1-SNAPSHOT.jar`** (~20 MB).

## 3. Arrancar
```
java -Xmx256m -jar target/backend-0.0.1-SNAPSHOT.jar
```
- Escucha **solo en `127.0.0.1:8080`** (loopback; Nginx hace el puente hacia afuera).
- **Configuración de la limpieza (Fase E)**, ajustable sin recompilar:
  - `juego.limpieza.timeout` (defecto `PT30M`): inactividad antes de borrar una partida.
  - `juego.limpieza.intervalo` (defecto `PT5M`): cada cuánto corre el barrido.
  - Override: `--juego.limpieza.timeout=PT20M` como argumento, o variable de entorno
    `JUEGO_LIMPIEZA_TIMEOUT=PT20M`.

## 4. Verificación en limpio (checklist)
Con el `.jar` corriendo bajo `-Xmx256m`:

| Qué | Comando | Esperado |
|---|---|---|
| Binding loopback | `netstat -ano \| grep :8080` | `127.0.0.1:8080` (NO `0.0.0.0`) |
| Salud | `curl --head http://127.0.0.1:8080/api/salud` | `200` sin cuerpo |
| Salud con parámetro | `curl --head "http://127.0.0.1:8080/api/salud?x=1"` | `409` |
| Salud por GET | `curl http://127.0.0.1:8080/api/salud` | `405` (solo HEAD) |
| 413 | POST con cuerpo > 4 KB a `/api/partidas` | `413` con cabecera CORS |
| CORS en error | GET con `Origin:` a un id inexistente | `404` con `Access-Control-Allow-Origin: *` |
| Cuerpo en iniciar/avanzar | POST `{"x":1}` + token a `/iniciar` | `409` |
| Flujo | crear + 3 uniones + `iniciar` | `200`, vista en fase `respuestas` |
| Memoria | `tasklist /FI "PID eq <pid>"` | RSS bajo 256 MB |

### Resultado medido (main + #29 + #31, 26 nov)
Todos los checks **en verde**. Memoria del proceso: **~160 MB** RSS con `-Xmx256m`.
Suite completa: **87 pruebas verdes**.

## 5. Entrega a Joel
1. Pasarle `target/backend-0.0.1-SNAPSHOT.jar` (o el nombre que use `deploy.ps1`).
2. Recordarle el comando de arranque: `java -Xmx256m -jar <jar>`.
3. Prueba de humo en la VM tras desplegar:
   `curl --head https://api-democraxssy-if6000.duckdns.org/api/salud` → `200`,
   y la misma por `http://...` (proxy).
4. Ensayo: una partida de 4 jugadores completa por HTTPS y por HTTP.

## 6. Importante
El `.jar` **final** debe incluir **#29** (cuerpo vacío en `iniciar`/`avanzar` → 409) y
**#31** (limpieza de partidas inactivas). Al 26 nov están **en revisión**, no en `main`.
**Cortar el `.jar` definitivo recién cuando ambos estén mergeados** y repetir esta
verificación sobre `main`.
