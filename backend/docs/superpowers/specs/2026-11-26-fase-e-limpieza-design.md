# Fase E — Limpieza de partidas inactivas (diseño)

**Fecha:** 2026-11-26 · **Autor:** José Andrés · **Tamaño:** cambio pequeño

## Intención y por qué
El estado vive en memoria y la VM corre con `-Xmx256m`. Partidas abandonadas
(nadie las consulta) se acumulan y pueden reventar la memoria. Queremos que se
borren solas cuando llevan un rato sin actividad.

## Objetivos
- Una partida sin actividad por más de un **umbral** se elimina automáticamente.
- Una partida que alguien está viendo **nunca** se borra: el frontend hace polling
  cada ~2 s, y cada petición cuenta como actividad.
- **Umbral** e **intervalo** de barrido configurables (Joel los ajusta en la VM sin
  recompilar).

## No-objetivos / anti-objetivos
- No limpiar por volumen (número de partidas) ni midiendo memoria: el problema es el
  abandono temporal, no la cantidad.
- No tocar la lógica del juego ni el contrato HTTP (ningún código nuevo cambia
  respuestas de las 10 operaciones).
- No meter el barrido en el camino caliente de cada request.

## Restricciones
- En memoria, sin base de datos. `-Xmx256m`. Concurrencia por `synchronized(partida)`.

## Enfoques considerados
1. **Timestamp de última actividad + barrido programado (elegido).** Cada partida
   recuerda cuándo se la tocó por última vez; un job periódico borra las que pasaron
   el umbral. Simple, aislado, no toca el camino caliente.
2. **Límite por cantidad (LRU / tope de N partidas).** Descartado: el problema no es
   volumen sino salas abandonadas; un tope arbitrario podría botar partidas vivas.
3. **Limpiar dentro de cada request (sin scheduler).** Descartado: mete latencia y
   lógica de barrido en cada operación.

## Diseño (lo decidido)
- **`Partida.ultimaActividad`**: campo `volatile Instant`, inicializado a `Instant.now()`
  en el constructor, con getter/setter. Dato puro (el dominio sigue sin lógica).
- **`RepositorioEnMemoria.obtener(id)`**: estampa `partida.ultimaActividad(Instant.now())`
  antes de devolver. Es el punto único por el que pasan todas las operaciones, así que
  cualquier petición (incluido el polling) renueva la actividad. `crearPartida` usa
  `guardar`, y el constructor ya deja la marca fresca.
- **`RepositorioPartidas.eliminarInactivas(Instant limite)`**: recorre el mapa y elimina
  las partidas con `ultimaActividad` anterior a `limite`; devuelve cuántas borró.
- **`LimpiezaPartidas`** (`@Component`): método `limpiar()` anotado con
  `@Scheduled(fixedDelayString = "${juego.limpieza.intervalo}")`, que llama
  `repositorio.eliminarInactivas(Instant.now().minus(timeout))` y loguea cuántas borró.
  El `timeout` se inyecta como `Duration` desde `${juego.limpieza.timeout}`.
- **`@EnableScheduling`** en `JuegoConfig`.
- **`application.properties`**: `juego.limpieza.timeout=PT30M`, `juego.limpieza.intervalo=PT5M`.

## Concurrencia
`ultimaActividad` es `volatile`: escritura y lectura de la referencia son atómicas, así
que estampar en `obtener()` fuera del lock es seguro. El barrido itera el
`ConcurrentHashMap` (iteración débilmente consistente) y borra por id; una lectura
ligeramente vieja solo retrasa el borrado un ciclo, nunca borra una partida viva.

## Qué queda al constructor (builder)
- Nombres exactos de las propiedades y los mensajes de log.
- Pruebas: `eliminarInactivas` (borra viejas, conserva frescas, cuenta), `obtener`
  renueva la marca, y `limpiar()` calcula el corte y delega en el repo.
