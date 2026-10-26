// src/state/partida.ts
//
// Capa de datos única del juego (responsabilidad de la persona A).
// Todas las pantallas LEEN de aquí con usePartida() y disparan acciones con las
// funciones exportadas; no llaman al cliente generado directamente.
//
// Patrón de store minimalista (igual que callLog): sin librerías externas,
// compatible con useSyncExternalStore.

import { useSyncExternalStore } from "react";
import axios from "axios";
import { ObtenerPartidaResponse } from "../schemas/generated/democraxssy.zod";
import { setAuthToken } from "../api/client";
import {
  headApiSalud,
  crearPartida,
  unirse,
  obtenerPartida,
  iniciar,
  responder,
  votar,
  decidir,
  avanzar,
  eliminarPartida,
} from "../api/generated/democraxssy";

// Tipos derivados de las funciones generadas: no dependemos del nombre exacto
// que orval le haya puesto a cada modelo.
export type Vista = Awaited<ReturnType<typeof obtenerPartida>>;
type Sesion = Awaited<ReturnType<typeof crearPartida>>;

type Estado = {
  partidaId: string | null;
  jugadorId: string | null;
  partida: Vista | null;
  error: string | null;
  cargando: boolean;
};

let estado: Estado = {
  partidaId: sessionStorage.getItem("partidaId"),
  jugadorId: sessionStorage.getItem("jugadorId"),
  partida: null,
  error: null,
  cargando: false,
};

const listeners = new Set<() => void>();
function emit() {
  listeners.forEach((l) => l());
}
function set(parcial: Partial<Estado>) {
  estado = { ...estado, ...parcial };
  emit();
}

export function getEstado(): Estado {
  return estado;
}
export function subscribe(l: () => void): () => void {
  listeners.add(l);
  return () => {
    listeners.delete(l);
  };
}
/** Hook para que los componentes lean el estado de forma reactiva. */
export function usePartida(): Estado {
  return useSyncExternalStore(subscribe, getEstado);
}

// Mensajes para la persona que juega, según el código HTTP. El texto del
// servidor se queda en el panel de llamadas y en la consola; aquí solo se usa
// cuando no hay un mensaje propio para ese código.
type Mensajes = Partial<Record<number, string>>;

const MENSAJES_COMUNES: Mensajes = {
  401: "Tu sesión en esta partida ya no es válida. Vuelve a la pantalla de acceso y únete de nuevo.",
  404: "La partida ya no existe o el ID no es correcto.",
  413: "La solicitud es demasiado grande para el servidor.",
  500: "El servidor tuvo un problema interno. Inténtalo de nuevo en unos segundos.",
};

function mensajeError(e: unknown, propios: Mensajes = {}): string {
  if (axios.isAxiosError<{ error?: string }>(e)) {
    const status = e.response?.status;
    if (status === undefined) {
      return "No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.";
    }
    console.warn(`El servidor respondió ${status}:`, e.response?.data?.error);
    return (
      propios[status] ??
      MENSAJES_COMUNES[status] ??
      (status === 409
        ? "Esa acción no corresponde al momento actual de la partida."
        : "El servidor rechazó la acción. Inténtalo de nuevo.")
    );
  }
  if (e instanceof RespuestaInvalida) {
    return "El servidor respondió algo que no es una partida. Revisa la dirección del backend.";
  }
  return "Ocurrió un error inesperado al comunicarse con el servidor.";
}

// Si el servidor devuelve algo que no es una partida (por ejemplo, una página
// HTML de otro servidor), se rechaza aquí y no llega a romper la pantalla.
class RespuestaInvalida extends Error {}

function validarVista(v: Vista): Vista {
  if (!ObtenerPartidaResponse.safeParse(v).success) {
    throw new RespuestaInvalida("La respuesta no tiene el formato de una partida.");
  }
  return v;
}

function guardarSesion(s: Sesion) {
  sessionStorage.setItem("partidaId", s.partida_id);
  sessionStorage.setItem("jugadorId", s.jugador_id);
  setAuthToken(s.token);
  set({
    partidaId: s.partida_id,
    jugadorId: s.jugador_id,
    partida: s.partida,
    error: null,
  });
}

/** Envuelve una acción que devuelve la Vista actualizada, manejando carga y error. */
async function accion(fn: () => Promise<Vista>, mensajes?: Mensajes) {
  set({ cargando: true, error: null });
  try {
    const v = validarVista(await fn());
    set({ partida: v });
  } catch (e) {
    // El error lo puso una acción, no el refresco: que el polling no lo borre.
    errorDeRefresco = false;
    set({ error: mensajeError(e, mensajes) });
    throw e;
  } finally {
    set({ cargando: false });
  }
}

// --- Acciones ---

/** Health check: aparece en el panel de llamadas. */
export async function probarSalud(): Promise<void> {
  await headApiSalud();
}

export async function crear(nombre: string, puntos: number): Promise<void> {
  set({ cargando: true, error: null });
  try {
    const s = await crearPartida({ nombre, puntos_para_ganar: puntos });
    guardarSesion(s);
  } catch (e) {
    set({
      error: mensajeError(e, {
        400: "El nombre debe tener entre 1 y 24 caracteres y los puntos deben estar entre 1 y 20.",
      }),
    });
    throw e;
  } finally {
    set({ cargando: false });
  }
}

export async function entrar(partidaId: string, nombre: string): Promise<void> {
  set({ cargando: true, error: null });
  try {
    const s = await unirse(partidaId, { nombre });
    guardarSesion(s);
  } catch (e) {
    set({
      error: mensajeError(e, {
        400: "El nombre debe tener entre 1 y 24 caracteres.",
        404: "No existe una partida con ese ID. Revisa que lo hayas copiado completo.",
        409: "No te puedes unir: ese nombre ya está en la mesa, la partida está llena o ya empezó.",
      }),
    });
    throw e;
  } finally {
    set({ cargando: false });
  }
}

// El refresco solo borra los errores que puso él mismo, para no
// tapar los mensajes de las acciones (por ejemplo, un 409).
let errorDeRefresco = false;

export async function refrescar(): Promise<void> {
  const partidaId = estado.partidaId;
  if (!partidaId) return;
  try {
    const v = validarVista(await obtenerPartida(partidaId));
    if (estado.partidaId !== partidaId) return;
    // Al cambiar de fase, el error de una acción anterior ya no aplica.
    const cambioDeFase = estado.partida !== null && estado.partida.fase !== v.fase;
    if (errorDeRefresco || cambioDeFase) {
      errorDeRefresco = false;
      set({ partida: v, error: null });
    } else {
      set({ partida: v });
    }
  } catch (e) {
    if (estado.partidaId !== partidaId) return;
    // La partida ya no existe: salir a la pantalla de acceso.
    if (axios.isAxiosError(e) && e.response?.status === 404) {
      limpiarSesion();
      set({ error: "La partida ya no existe: el anfitrión la eliminó." });
      return;
    }
    // Al recargar la pestaña, si la sesión guardada ya no sirve (token
    // inválido, partida de otro servidor…), se borra en vez de reintentar
    // para siempre sin mostrar nada.
    // (Un corte de red no cuenta: ahí sí se sigue reintentando.)
    const rechazada = axios.isAxiosError(e) ? e.response !== undefined : true;
    if (estado.partida === null && rechazada) {
      limpiarSesion();
      set({ error: "No se pudo recuperar tu sesión en la partida. Vuelve a unirte con el ID." });
      return;
    }
    errorDeRefresco = true;
    set({ error: mensajeError(e) });
  }
}

/** Solo el anfitrión, en fase sala, con >=4 jugadores en número par. Body vacío. */
export async function comenzar(): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => iniciar(estado.partidaId as string, {}),
    {
      403: "Solo el anfitrión puede repartir las cartas.",
      409: "Para iniciar se necesitan al menos 4 jugadores y un número par.",
    },
  );
}

export async function enviarRespuesta(cartas: number[]): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => responder(estado.partidaId as string, { cartas }),
    {
      400: "Esa no es la cantidad de cartas que pide la carta negra.",
      403: "El soberano no baja cartas en esta ronda.",
      409: "Ya bajaste tu respuesta en esta ronda.",
    },
  );
}

export async function emitirVoto(respuestaId: string): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => votar(estado.partidaId as string, { respuesta_id: respuestaId }),
    {
      403: "El soberano no vota en esta ronda.",
      409: "Ya registraste tu voto en esta ronda.",
    },
  );
}

export async function elegirGanadora(respuestaId: string): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => decidir(estado.partidaId as string, { respuesta_id: respuestaId }),
    {
      403: "Solo el soberano elige la respuesta ganadora.",
      409: "La ganadora de esta ronda ya fue elegida.",
    },
  );
}

/** Pasa a la siguiente fase/ronda. Body vacío. */
export async function avanzarRonda(): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => avanzar(estado.partidaId as string, {}),
    {
      403: "Solo el anfitrión o el soberano pueden pasar a la siguiente ronda.",
      409: "La ronda todavía no se puede avanzar.",
    },
  );
}

export async function eliminar(): Promise<void> {
  if (!estado.partidaId) return;
  try {
    await eliminarPartida(estado.partidaId);
    limpiarSesion();
  } catch (e) {
    set({
      error: mensajeError(e, {
        403: "Solo el anfitrión puede eliminar la partida.",
        409: "La partida solo se puede eliminar cuando ya terminó.",
      }),
    });
    throw e;
  }
}

export function limpiarSesion(): void {
  sessionStorage.removeItem("partidaId");
  sessionStorage.removeItem("jugadorId");
  setAuthToken(null);
  set({ partidaId: null, jugadorId: null, partida: null, error: null });
}

// --- Polling: para ver las acciones de los otros jugadores ---
let timer: number | null = null;
export function iniciarPolling(ms = 2000): void {
  detenerPolling();
  timer = window.setInterval(refrescar, ms);
  void refrescar();
}
export function detenerPolling(): void {
  if (timer !== null) {
    clearInterval(timer);
    timer = null;
  }
}
