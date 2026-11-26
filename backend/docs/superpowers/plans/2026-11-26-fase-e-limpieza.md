# Fase E — Limpieza de partidas inactivas (plan)

Diseño: `../specs/2026-11-26-fase-e-limpieza-design.md`. TDD, un paso a la vez.

## Tarea 1 — `Partida.ultimaActividad`
- Test: una partida nueva tiene `ultimaActividad` cercana a ahora; el setter la cambia.
- Impl: campo `volatile Instant ultimaActividad` = `Instant.now()` en el constructor; getter/setter.

## Tarea 2 — `obtener()` renueva la actividad
- Test: fijar `ultimaActividad` a un instante viejo, llamar `repositorio.obtener(id)`,
  verificar que quedó renovada (posterior al valor viejo).
- Impl: en `RepositorioEnMemoria.obtener`, estampar `Instant.now()` antes de devolver.

## Tarea 3 — `eliminarInactivas(Instant limite)`
- Test: con una partida vieja y una fresca, `eliminarInactivas(corte)` borra solo la vieja
  y devuelve 1; la fresca sigue consultable.
- Impl: método en la interfaz + `RepositorioEnMemoria` que recorre el mapa y elimina las
  de `ultimaActividad` anterior a `limite`.

## Tarea 4 — `LimpiezaPartidas` (barrido)
- Test: con un repo stub, `limpiar()` llama `eliminarInactivas` con un corte ≈ ahora − timeout.
- Impl: `@Component` con `limpiar()` `@Scheduled(fixedDelayString=...)`, `timeout` por `@Value Duration`.

## Tarea 5 — Wiring y config
- `@EnableScheduling` en `JuegoConfig`; propiedades `juego.limpieza.timeout=PT30M`,
  `juego.limpieza.intervalo=PT5M` en `application.properties`.
- Smoke test con timeout corto (override por línea de comandos): crear partida, esperar,
  GET → 404.
