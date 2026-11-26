# Prueba de memoria del backend

Backend completo (fases B y C, 413, claves repetidas y handler del 500), desplegado el 25 nov 2026 con `deploy.ps1`. Recién desplegado, el servicio usaba 170 MB.
Se repite con `powershell -ExecutionPolicy Bypass -File tools\prueba-memoria.ps1 [-Partidas N] [-EsperarMin M]`.

Cada partida tiene 22 jugadores (el máximo del contrato) y está iniciada: cada jugador tiene su mano de 7 cartas y hay una ronda abierta. Las dos corridas se hicieron seguidas, así que la segunda empezó con las 20 partidas de la primera todavía abiertas.

## Corrida 1: 20 partidas (440 jugadores), 25 nov 21:24

| Momento | Servicio (MB) | Tope del servicio (MB) | RAM usada de la VM (MB) |
|---|---:|---:|---:|
| Antes de la carga | 177 | 500 | 569 de 945 |
| Pico durante la carga | 183 | 500 | 563 de 945 |
| Con las partidas abiertas | 183 | 500 | 564 de 945 |

Carga: `{"partidas":20,"jugadores":440,"iniciadas":20,"codigos":{"200":20,"201":440},"segundos":41}`

## Corrida 2: 80 partidas más (1760 jugadores; 100 partidas y 2200 jugadores en total), 25 nov 21:27

| Momento | Servicio (MB) | Tope del servicio (MB) | RAM usada de la VM (MB) |
|---|---:|---:|---:|
| Antes de la carga | 183 | 500 | 575 de 945 |
| Pico durante la carga | 187 | 500 | 565 de 945 |
| Con las partidas abiertas | 187 | 500 | 568 de 945 |

Carga: `{"partidas":80,"jugadores":1760,"iniciadas":80,"codigos":{"200":80,"201":1760},"segundos":164}`

## Qué se concluye

- **Con 100 partidas llenas abiertas a la vez, el servicio pasó de 177 a 187 MB.** Es poco más de un tercio del tope de 500 MB de systemd, y la VM conservó unos 380 MB libres.
- **Las 2300 solicitudes de las dos corridas respondieron bien** (todas 200 o 201), sin errores ni 503 del rate limit de Nginx (se envían a 12 por segundo).
- **El techo real lo pone la JVM:** el heap no pasa de 256 MB (`-Xmx256m`) y el metaspace de 128 MB. Aunque la carga creciera mucho más, la JVM respondería con un error de memoria propio antes de que systemd mate el servicio por llegar a 500 MB.
- **Pendiente:** la limpieza de partidas inactivas (fase E de José). Cuando esté, se repite con `-EsperarMin` para mostrar que la memoria baja sola. Mientras tanto, las partidas de carga se borraron reiniciando el servicio (`deploy.ps1` del mismo `.jar`).
