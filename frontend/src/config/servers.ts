const API_HOST = "api-democraxssy-if6000.duckdns.org";

export const SERVER_PRESETS = {
  ownBackendHttps: {
    label: "Nuestro backend (HTTPS)",
    url: `https://${API_HOST}`,
  },
  ownBackendHttp: {
    label: "Nuestro backend (HTTP vía proxy)",
    url: `http://${API_HOST}`,
  },
  professorServer: {
    label: "Servidor del profesor",
    url: "https://democraxssy.akamai.meseguercr.com",
  },
} as const;
