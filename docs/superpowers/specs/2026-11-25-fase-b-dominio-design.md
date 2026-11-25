# Fase B — Modelo de dominio y máquina de estados (diseño)

**Proyecto:** democraXSSy backend · **Fecha:** 2026-11-25
**Fuente de verdad del shape de datos:** `openapi.yaml` (raíz del repo).

## Intención y por qué

Construir el núcleo del juego **en memoria y bien testeado antes de exponerlo por HTTP**
(Fase C). Es la parte con más lógica y más riesgo (reglas, estados, puntuación), así que va
aislada del framework y con TDD. El criterio de éxito de la fase: poder **simular una partida
completa en tests sin tocar HTTP**.

## Objetivos

- Modelo de dominio completo según `openapi.yaml`.
- Máquina de estados explícita: transiciones válidas, y las inválidas se **rechazan con
  excepciones tipadas** (base de los 409/403/400 de la Fase D).
- Revelado progresivo por fase (no filtrar manos ajenas, votos ni autores antes de tiempo).
- Puntuación: `puntos`, `favoritas_del_pueblo`, `respeto_al_pueblo`.
- Rotación del soberano por ronda.
- Mazos desde `cartas.json` con **reciclaje de descartes** (la partida termina solo por puntos).
- Tests unitarios que simulan partidas completas.

## No-objetivos (otras fases)

- Capa HTTP / controllers / mapeo de códigos de estado → **Fase C/D**.
- Formato de error `{"error": "mensaje en español"}`, rechazo de query params, cabeceras y
  claves JSON repetidas, límite de 4 KB (413) → **Fase D**.
- Limpieza de partidas inactivas / timeouts → **Fase E**.
- Persistencia: nunca; el estado vive en memoria.

## Anti-objetivos (fracaso aunque "funcione")

- Lógica de juego entrelazada con Spring (no testeable sin levantar contexto).
- Filtrar información oculta (manos ajenas, votos antes de `decision`, autores antes de `resultado`).
- Estado que crece sin tope (mazos que no reciclan) → revienta los 256 MB.

## Restricciones

- Java 21. El dominio **no usa Spring** ni dependencias nuevas.
- `-Xmx256m`: estructuras acotadas, reciclar mazos.
- Varios jugadores actúan sobre la **misma** partida de forma concurrente.

## Enfoque (approaches considerados)

1. **Por capas con dominio anémico** *(elegido)*: `dominio/` tiene solo **entidades (datos)** y
   `servicio/` tiene **la lógica** (`JuegoServicio`: motor de estados, reglas, puntuación, mazo,
   revelado). El `servicio` es Java plano, sin dependencias web; se instancia directo en los tests,
   **sin contexto de Spring**. En Fase C puede llevar `@Service` para el wiring. Preserva el objetivo
   de la fase (testear sin HTTP) y es la estructura que el equipo prefiere.
2. **Dominio rico (lógica en las entidades)** *(descartado)*: cohesivo, pero el equipo prefiere
   separar entidades de lógica en capas.
3. **Event sourcing / command bus** *(descartado)*: sobreingeniería para estado en memoria de un
   proyecto de curso.

## Decisiones de diseño (el "cómo")

### Estructura de paquetes
```
org.democraxssy
├── dominio/       entidades (datos): Partida, Jugador, Ronda, Carta*, Respuesta, ResumenRonda, Fase, Mazo, Mazos
├── dto/           DTOs del contrato (paquete neutral): Sesion, Vista, JugadorPublico, Yo, Ronda, RespuestaRevelada, ResumenRonda
├── servicio/      lógica: JuegoServicio (interfaz) + JuegoEnMemoria (impl) + Ids, CargadorMazo
├── repositorio/   RepositorioPartidas (interface) + RepositorioEnMemoria
├── controllers/   @RestController HTTP (Fase C) + SaludController
├── web/           CorsFilter, ManejadorErrores (sin capa de mapeo)
├── errores/       NoAutenticado, Prohibido, DatosInvalidos, NoEncontrado, Conflicto
└── config/        wiring (cargar cartas.json, beans)
```
Las entidades de `dominio/` son datos. Toda la lógica vive en `servicio/`. **`JuegoServicio` es una
interfaz** (impl `JuegoEnMemoria`) para que la capa web la mockee en sus tests. El servicio **devuelve
los DTOs de `dto/` ya filtrados por jugador** (revelado en un solo lugar); Jackson los serializa directo
al contrato, así que no hay capa de mapeo. Acordado con Joel (controllers de Fase C divididos).

### Identificadores
`partida_id` 32 hex · `jugador_id` 16 hex · `token` 64 hex · `respuesta_id` 12 hex. Generados con
`SecureRandom`. Las `CartaBlanca` llevan el `id` del mazo (int ≥ 0).

### Máquina de estados
Fases: `sala → respuestas → votacion → decision → resultado → (respuestas | terminada)`.

| Desde | Transición | Quién / condición | Efecto |
|---|---|---|---|
| sala | `iniciar` | anfitrión; 4–22 jugadores en número **par** | soberano = anfitrión; reparte 7 cartas; ronda 1 → `respuestas` |
| respuestas | auto | todos los no-soberanos respondieron | → `votacion` (baraja respuestas una vez) |
| respuestas | `avanzar` | anfitrión/soberano; ≥ 2 respuestas | → `votacion` |
| votacion | auto | todos los no-soberanos votaron | → `decision` |
| votacion | `avanzar` | anfitrión/soberano | cierra con los votos que haya → `decision` |
| decision | `decidir` | soberano elige una respuesta | +1 al autor; calcula favoritas y respeto; al historial → `resultado` o `terminada` |
| resultado | `avanzar` | anfitrión/soberano | rota soberano (orden de llegada); repone manos a 7; nueva ronda → `respuestas` |
| terminada | — | — | final; solo consultar / eliminar |

Cualquier acción fuera de su fase o rol lanza excepción tipada (no cambia estado).

### Reglas finas (del contrato)
- El soberano **no** responde ni vota.
- Responder: exactamente `ronda.carta_negra.elegir` cartas de la propia mano, sin repetir; **el orden importa**.
- Votar: una sola vez; **se permite** votar la propia respuesta.
- Nombres: trim, 1–24, sin caracteres de control, **únicos sin distinguir mayúsculas**.
- `favoritas_del_pueblo` = respuestas con más votos (empates incluidos); se calcula al entrar a `decision`.
- `respeto_al_pueblo` = la ganadora estaba entre las favoritas, **o nadie votó**.
- `frase` de una respuesta = `carta_negra.texto` con cada `____` reemplazado, **en orden**, por el
  `texto` de las cartas jugadas (tantos huecos como `elegir`). *(Call del diseño.)*
- `puntos_para_ganar`: 1–20, **default 5** cuando llega `null`, según el contrato. *(Call del diseño.)*

### Revelado progresivo
- `respuestas` ocultas durante la fase `respuestas`.
- Desde `votacion`: reveladas **barajadas y anónimas** (se baraja **una vez** al abrir la votación, orden estable).
- `votos` por respuesta: desde `decision`.
- `autor_id`: desde `resultado`.
- `es_mia`: siempre disponible para el autor.

### Mazos (decisión #2, cerrada)
- Cargar `cartas.json` de `resources` al arrancar; validar (blancas `id`+`texto`; negras
  `texto`+`elegir` con **nº de `____` == `elegir`**); **fallar rápido** si el mazo es inválido.
- Blanco y negro tienen **montón de robo + descartes**. Al repartir/reponer, si el robo se vacía
  se **rebarajan los descartes**. Las manos se reponen a 7 tras cada ronda.
- La partida termina **solo por puntos**, nunca por falta de cartas.
- Mínimo de mazo validado para una mesa llena (≈ 22 × 7 blancas + negras suficientes).

### Concurrencia
- **Lock por partida**: todas las mutaciones de una partida se serializan (synchronized sobre la
  partida, o un lock por id en el registro). Suficiente y correcto a esta escala.

### Excepciones de dominio (base de la Fase D)
`ConflictoException` (fase/estado inválido, duplicados → 409) · `ProhibidoException` (rol → 403) ·
`DatosInvalidosException` (valores → 400) · `NoEncontradoException` (404). La Fase D las mapea al
formato `{"error": ...}` del contrato.

### Registro en memoria
`interface RepositorioPartidas` + implementación con `ConcurrentHashMap`. La Fase E le añade la
limpieza de partidas inactivas.

## Qué queda al builder

- Nombres exactos de clases/métodos y estructura fina de paquetes.
- La **proyección a los DTOs del contrato** (`Vista`, `Yo`, `JugadorPublico`, `RespuestaRevelada`)
  puede vivir en el borde (Fase C); el dominio expone lo necesario para construirla por jugador sin filtrar.
- Barajado con **semilla inyectable** para tests deterministas (recomendado).

## Plan de pruebas (TDD)

- Transiciones válidas e inválidas (una prueba por rechazo).
- Partida completa de 4: iniciar → responder×3 → votacion → votar×3 → decision → decidir →
  resultado → avanzar → nueva ronda.
- Puntuación y fin por puntos.
- `favoritas_del_pueblo` con empate; `respeto_al_pueblo` true/false y el caso "nadie votó".
- Reciclaje de mazo (mazo pequeño forzado, varias rondas, nunca termina por cartas).
- Revelado progresivo (no filtra manos/votos/autores antes de tiempo).
- Rotación del soberano en orden de llegada.
