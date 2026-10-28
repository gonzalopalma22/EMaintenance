import { useEffect, useState } from "react";
import { useMsal } from "@azure/msal-react";
import { loginRequest } from "./authConfig";

function decodificar(jwt) {
  try {
    const payload = jwt.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch {
    return {};
  }
}

export function useRoles() {
  const { instance, accounts } = useMsal();
  const [roles, setRoles] = useState([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let activo = true;
    const account = instance.getActiveAccount() ?? accounts[0];
    if (!account) {
      setRoles([]);
      setCargando(false);
      return;
    }
    instance
      .acquireTokenSilent({ ...loginRequest, account })
      .then((r) => {
        if (!activo) return;
        const claims = decodificar(r.accessToken);
        setRoles(Array.isArray(claims.roles) ? claims.roles : []);
      })
      .catch(() => activo && setRoles([]))
      .finally(() => activo && setCargando(false));
    return () => {
      activo = false;
    };
  }, [instance, accounts]);

  return { roles, esAdmin: roles.includes("Admin"), cargando };
}