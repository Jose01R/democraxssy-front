// src/components/DecoracionMesa.tsx
//
// Objetos decorativos de la mesa de cartas, dibujados como SVG en línea
// (sin imágenes externas, para respetar la CSP). Son solo decoración:
// aria-hidden para que los lectores de pantalla los ignoren.

export function Cenicero() {
  return (
    <svg className="deco deco-cenicero" viewBox="0 0 120 120" aria-hidden="true">
      <circle cx="60" cy="60" r="46" fill="#3b3430" />
      <circle cx="60" cy="60" r="46" fill="none" stroke="#5a504a" strokeWidth="3" />
      <circle cx="60" cy="60" r="32" fill="#1f1a17" />
      <circle cx="60" cy="60" r="32" fill="none" stroke="#2b2420" strokeWidth="4" />
      {/* Muescas del borde */}
      <rect x="54" y="10" width="12" height="10" rx="4" fill="#1f1a17" />
      <rect x="54" y="100" width="12" height="10" rx="4" fill="#1f1a17" />
      <rect x="10" y="54" width="10" height="12" rx="4" fill="#1f1a17" />
      <rect x="100" y="54" width="10" height="12" rx="4" fill="#1f1a17" />
      {/* Ceniza */}
      <circle cx="52" cy="66" r="6" fill="#55504c" />
      <circle cx="66" cy="58" r="4" fill="#4a4542" />
      {/* Cigarro apoyado en la muesca */}
      <g transform="rotate(-35 60 60)">
        <rect x="58" y="-2" width="7" height="30" rx="2" fill="#efe7dc" />
        <rect x="58" y="-2" width="7" height="9" rx="2" fill="#c9823a" />
        <rect x="58" y="26" width="7" height="3" fill="#7c766f" />
      </g>
      <path className="deco-humo" d="M86 22 q-6 -8 0 -14 q6 -6 0 -14" fill="none" stroke="#a8a29c" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function TazaCafe() {
  return (
    <svg className="deco deco-cafe" viewBox="0 0 120 120" aria-hidden="true">
      {/* Plato */}
      <circle cx="58" cy="62" r="44" fill="#e9e2d8" />
      <circle cx="58" cy="62" r="34" fill="none" stroke="#d6cdc1" strokeWidth="2" />
      {/* Asa */}
      <rect x="86" y="54" width="22" height="14" rx="7" fill="#f6f1ea" stroke="#d6cdc1" strokeWidth="2" />
      {/* Taza vista desde arriba */}
      <circle cx="58" cy="62" r="28" fill="#f6f1ea" stroke="#d6cdc1" strokeWidth="2" />
      <circle cx="58" cy="62" r="22" fill="#4a2c1a" />
      <circle cx="58" cy="62" r="22" fill="none" stroke="#6b4228" strokeWidth="3" />
      <path d="M48 58 q10 -8 20 0 q-10 10 -20 0" fill="#c8a27a" opacity="0.7" />
      {/* Cuchara */}
      <rect x="14" y="88" width="34" height="5" rx="2.5" fill="#b9b2a8" transform="rotate(-25 31 90)" />
    </svg>
  );
}

export function JarraCerveza() {
  return (
    <svg className="deco deco-cerveza" viewBox="0 0 120 120" aria-hidden="true">
      {/* Posavasos */}
      <rect x="12" y="16" width="90" height="90" rx="10" fill="#7a2e22" />
      <rect x="18" y="22" width="78" height="78" rx="7" fill="none" stroke="#e0b43a" strokeWidth="2" strokeDasharray="4 4" />
      {/* Asa */}
      <rect x="90" y="52" width="22" height="18" rx="8" fill="none" stroke="#cfd8dc" strokeWidth="6" />
      {/* Vaso visto desde arriba: espuma y cerveza */}
      <circle cx="57" cy="61" r="32" fill="#d9a531" stroke="#cfd8dc" strokeWidth="4" />
      <circle cx="57" cy="61" r="24" fill="#fff4dc" />
      <circle cx="49" cy="55" r="5" fill="#ffffff" />
      <circle cx="64" cy="66" r="4" fill="#ffffff" />
      <circle cx="60" cy="52" r="3" fill="#ffffff" />
    </svg>
  );
}

/**
 * Montón de respuestas boca abajo: una carta por cada respuesta que llegó.
 * Cada carta nueva "cae" sobre la mesa con una animación corta.
 */
export function PilaRespuestas({ cantidad }: { cantidad: number }) {
  const visibles = Math.min(cantidad, 7);
  return (
    <div className="pila" aria-hidden="true">
      {visibles === 0 && <span className="pila-vacia">Aquí caen las cartas</span>}
      {Array.from({ length: visibles }, (_, i) => (
        <span
          key={i}
          className="pila-carta"
          style={{ ["--i" as string]: i, ["--giro" as string]: `${((i * 37) % 17) - 8}deg` }}
        >
          {i === visibles - 1 && <span className="material-symbols-outlined">style</span>}
        </span>
      ))}
    </div>
  );
}
