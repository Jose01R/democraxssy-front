// src/api/callLog.ts
//
// Registro en memoria de las llamadas HTTP, para mostrarlas en el panel de la UI
// (además de la pestaña Network del navegador). Es un store minimalista sin
// librerías: guarda las últimas N llamadas y avisa a quien esté suscrito.

export type CallRecord = {
  id: number;
  method: string;
  url: string;
  status: number; // 0 si no hubo respuesta (error de red / CORS)
  ms: number;
  ok: boolean;
  at: string; // hora legible
};

const MAX = 100;

let records: CallRecord[] = [];
let counter = 0;
const listeners = new Set<() => void>();

export function recordCall(r: Omit<CallRecord, "id" | "at">): void {
  const rec: CallRecord = { ...r, id: ++counter, at: new Date().toLocaleTimeString() };
  records = [rec, ...records].slice(0, MAX); // más reciente primero
  listeners.forEach((l) => l());
}

/** Snapshot. Cambia de identidad en cada recordCall, apto para useSyncExternalStore. */
export function getCalls(): CallRecord[] {
  return records;
}

export function clearCalls(): void {
  records = [];
  listeners.forEach((l) => l());
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}
