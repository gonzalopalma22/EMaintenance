import { useMemo } from "react";
import { useMsal } from "@azure/msal-react";
import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { API_BASE_URL, loginRequest } from "./authConfig";

export function useApi() {
  const { instance, accounts } = useMsal();

  return useMemo(() => {
    async function getToken() {
      const account = instance.getActiveAccount() ?? accounts[0];
      try {
        const r = await instance.acquireTokenSilent({ ...loginRequest, account });
        return r.accessToken;
      } catch (e) {
        if (e instanceof InteractionRequiredAuthError) {
          await instance.acquireTokenRedirect({ ...loginRequest, account });
          return null;
        }
        throw e;
      }
    }

    async function request(method, path, body) {
      const token = await getToken();
      if (!token) return null;

      const res = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers: {
          Authorization: `Bearer ${token}`,
          ...(body ? { "Content-Type": "application/json" } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
      });

      if (!res.ok) {
        const texto = await res.text();
        const err = new Error(`HTTP ${res.status} ${texto}`.trim());
        err.status = res.status;
        throw err;
      }
      if (res.status === 204) return null;

      const tipo = res.headers.get("content-type") ?? "";
      return tipo.includes("application/json") ? res.json() : res.text();
    }

    return {
      get: (path) => request("GET", path),
      post: (path, body) => request("POST", path, body),
      patch: (path, body) => request("PATCH", path, body),
      del: (path) => request("DELETE", path),
    };
  }, [instance, accounts]);
}