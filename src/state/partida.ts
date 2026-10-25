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

function mensajeError(e: unknown, guia409?: string): string {
  if (axios.isAxiosError<{ error?: string }>(e)) {
    if (e.response?.status === 409) {
      const guia =
        guia409 ??
        "Verifica que la acción corresponda a la fase actual de la partida y que la solicitud respete el contrato de la API.";
      return `El servidor rechazó la acción (409). ${guia}`;
    }
    return (
      e.response?.data?.error ??
      "No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo."
    );
  }
  return "Ocurrió un error inesperado al comunicarse con el servidor.";
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
async function accion(fn: () => Promise<Vista>, guia409?: string) {
  set({ cargando: true, error: null });
  try {
    const v = await fn();
    set({ partida: v });
  } catch (e) {
    set({ error: mensajeError(e, guia409) });
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
    set({ error: mensajeError(e) });
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
    set({ error: mensajeError(e) });
    throw e;
  } finally {
    set({ cargando: false });
  }
}

export async function refrescar(): Promise<void> {
  const partidaId = estado.partidaId;
  if (!partidaId) return;
  try {
    const v = await obtenerPartida(partidaId);
    if (estado.partidaId !== partidaId) return;
    set({ partida: v, error: null });
  } catch (e) {
    if (estado.partidaId !== partidaId) return;
    set({ error: mensajeError(e) });
  }
}

/** Solo el anfitrión, en fase sala, con >=4 jugadores en número par. Body vacío. */
export async function comenzar(): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => iniciar(estado.partidaId as string, {}),
    "La partida solo puede iniciar desde la sala con al menos 4 jugadores y un número par.",
  );
}

export async function enviarRespuesta(cartas: number[]): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => responder(estado.partidaId as string, { cartas }),
    "Todavía no es tu turno para responder o la fase de respuestas aún no está abierta.",
  );
}

export async function emitirVoto(respuestaId: string): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => votar(estado.partidaId as string, { respuesta_id: respuestaId }),
    "La votación aún no está abierta o ya registraste tu voto.",
  );
}

export async function elegirGanadora(respuestaId: string): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => decidir(estado.partidaId as string, { respuesta_id: respuestaId }),
    "Todavía no es tu turno para elegir la respuesta ganadora; solo el soberano decide en esta fase.",
  );
}

/** Pasa a la siguiente fase/ronda. Body vacío. */
export async function avanzarRonda(): Promise<void> {
  if (!estado.partidaId) return;
  await accion(
    () => avanzar(estado.partidaId as string, {}),
    "La ronda aún no está lista para avanzar o esta acción no te corresponde.",
  );
}

export async function eliminar(): Promise<void> {
  if (!estado.partidaId) return;
  try {
    await eliminarPartida(estado.partidaId);
    limpiarSesion();
  } catch (e) {
    set({
      error: mensajeError(
        e,
        "Solo el anfitrión puede eliminar la partida cuando ya terminó.",
      ),
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
