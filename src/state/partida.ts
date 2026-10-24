// src/state/partida.ts
//
// Capa de datos única del juego (responsabilidad de la persona A).
// Todas las pantallas LEEN de aquí con usePartida() y disparan acciones con las
// funciones exportadas; no llaman al cliente generado directamente.
//
// Patrón de store minimalista (igual que callLog): sin librerías externas,
// compatible con useSyncExternalStore.

import { useSyncExternalStore } from "react";
import type { AxiosError } from "axios";
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

function mensajeError(e: unknown): string {
  const err = e as AxiosError<{ error?: string }>;
  return err.response?.data?.error ?? "No se pudo conectar con el servidor.";
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
async function accion(fn: () => Promise<Vista>) {
  set({ cargando: true, error: null });
  try {
    const v = await fn();
    set({ partida: v });
  } catch (e) {
    set({ error: mensajeError(e) });
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
  await accion(() => iniciar(estado.partidaId as string, {}));
}

export async function enviarRespuesta(cartas: number[]): Promise<void> {
  if (!estado.partidaId) return;
  await accion(() => responder(estado.partidaId as string, { cartas }));
}

export async function emitirVoto(respuestaId: string): Promise<void> {
  if (!estado.partidaId) return;
  await accion(() => votar(estado.partidaId as string, { respuesta_id: respuestaId }));
}

export async function elegirGanadora(respuestaId: string): Promise<void> {
  if (!estado.partidaId) return;
  await accion(() => decidir(estado.partidaId as string, { respuesta_id: respuestaId }));
}

/** Pasa a la siguiente fase/ronda. Body vacío. */
export async function avanzarRonda(): Promise<void> {
  if (!estado.partidaId) return;
  await accion(() => avanzar(estado.partidaId as string, {}));
}

export async function eliminar(): Promise<void> {
  if (!estado.partidaId) return;
  await eliminarPartida(estado.partidaId);
  limpiarSesion();
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
