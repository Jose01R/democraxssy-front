# Prueba de memoria del backend

Se repite con `powershell -ExecutionPolicy Bypass -File tools\prueba-memoria.ps1 [-Partidas N] [-EsperarMin M]`.

Cada partida tiene 22 jugadores (el máximo del contrato) y está iniciada: cada jugador tiene su mano de 7 cartas y hay una ronda abierta.

## `.jar` final, con limpieza de partidas inactivas (25 nov, 22:49)

`.jar` de `main` en `90de7eb`, recién desplegado. Se abrieron 50 partidas (1100 jugadores) y se esperaron 36 minutos sin tocarlas: la limpieza borra las partidas sin actividad por 30 minutos y revisa cada 5.

| Momento | Servicio (MB) | Tope del servicio (MB) | RAM usada de la VM (MB) |
|---|---:|---:|---:|
| Antes de la carga | 154 | 500 | 559 de 945 |
| Pico durante la carga | 165 | 500 | 575 de 945 |
| Con las partidas abiertas | 165 | 500 | 571 de 945 |
| Tras 36 min sin actividad | 169 | 500 | 585 de 945 |

Carga: `{"partidas":50,"jugadores":1100,"iniciadas":50,"codigos":{"200":50,"201":1100},"segundos":100}`

Log del servicio (`journalctl -u democraxssy-back`):
```
2026-11-26T05:24:36Z INFO ... o.democraxssy.servicio.LimpiezaPartidas : Limpieza: 56 partida(s) inactiva(s) eliminada(s)
```
Fueron 56 y no 50 porque también se borraron otras 6 que habían quedado abiertas de las pruebas de esa noche (la prueba de contrato deja partidas en `sala`).

**La memoria no baja después de la limpieza, y es lo esperado.** La JVM conserva el heap que ya reservó en vez de devolverlo al sistema. Las partidas borradas sí liberan ese espacio dentro de la JVM, y las partidas nuevas lo reutilizan. Lo importante es que el uso queda acotado: no crece sin límite con partidas abandonadas.

## Juego sin validación ni limpieza, corrida 1: 20 partidas (440 jugadores), 25 nov 21:24

| Momento | Servicio (MB) | Tope del servicio (MB) | RAM usada de la VM (MB) |
|---|---:|---:|---:|
| Antes de la carga | 177 | 500 | 569 de 945 |
| Pico durante la carga | 183 | 500 | 563 de 945 |
| Con las partidas abiertas | 183 | 500 | 564 de 945 |

Carga: `{"partidas":20,"jugadores":440,"iniciadas":20,"codigos":{"200":20,"201":440},"segundos":41}`

## Juego sin validación ni limpieza, corrida 2: 80 partidas más (1760 jugadores; 100 partidas y 2200 jugadores en total), 25 nov 21:27

| Momento | Servicio (MB) | Tope del servicio (MB) | RAM usada de la VM (MB) |
|---|---:|---:|---:|
| Antes de la carga | 183 | 500 | 575 de 945 |
| Pico durante la carga | 187 | 500 | 565 de 945 |
| Con las partidas abiertas | 187 | 500 | 568 de 945 |

Carga: `{"partidas":80,"jugadores":1760,"iniciadas":80,"codigos":{"200":80,"201":1760},"segundos":164}`

Esas dos corridas se hicieron seguidas con el `.jar` anterior, así que la segunda empezó con las 20 partidas de la primera todavía abiertas. Las partidas se borraron reiniciando el servicio, porque ese `.jar` aún no tenía la limpieza.

## Qué se concluye

- **Con 100 partidas llenas abiertas a la vez, el servicio pasó de 177 a 187 MB.** Es poco más de un tercio del tope de 500 MB de systemd, y la VM conservó unos 380 MB libres.
- **Las 2300 solicitudes de las dos corridas respondieron bien** (todas 200 o 201), sin errores ni 503 del rate limit de Nginx (se envían a 12 por segundo).
- **El techo real lo pone la JVM:** el heap no pasa de 256 MB (`-Xmx256m`) y el metaspace de 128 MB. Aunque la carga creciera mucho más, la JVM respondería con un error de memoria propio antes de que systemd mate el servicio por llegar a 500 MB.
- **La limpieza de partidas inactivas funciona en producción:** borró las partidas abandonadas a los 30-35 minutos.
