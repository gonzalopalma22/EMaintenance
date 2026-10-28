export const msalConfig = {
  auth: {
    clientId: import.meta.env.VITE_SPA_CLIENT_ID,
    authority: `https://login.microsoftonline.com/${import.meta.env.VITE_TENANT_ID}`,
    redirectUri: window.location.origin,
  },
  cache: { cacheLocation: "sessionStorage" },
};

// Scope de la API que registraste en Azure
export const apiScopes = [
  `api://${import.meta.env.VITE_API_CLIENT_ID}/access_as_user`,
];

export const loginRequest = { scopes: apiScopes };

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;