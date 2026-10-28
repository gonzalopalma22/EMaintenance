import { useEffect, useState } from "react";
import {
  AuthenticatedTemplate,
  UnauthenticatedTemplate,
  useMsal,
} from "@azure/msal-react";
import "./index.css";
import { loginRequest } from "./authConfig";
import { useRoles } from "./useRoles";
import {
  IconOrdenes,
  IconLecturas,
  IconAlertas,
  IconHerramienta,
  IconSimulador,
  IconRabbit,
  IconLogo,
} from "./icons";
import Lecturas from "./pages/Lecturas";
import Alertas from "./pages/Alertas";
import Ordenes from "./pages/Ordenes";
import MisOrdenes from "./pages/MisOrdenes";
import Simulador from "./pages/Simulador";
import Rabbit from "./pages/Rabbit";

const PAGINAS = {
  misOrdenes: { titulo: "Mis órdenes", Icono: IconOrdenes, componente: MisOrdenes, roles: ["Usuario"] },
  lecturas: { titulo: "Lecturas", Icono: IconLecturas, componente: Lecturas, roles: ["Usuario", "Admin"] },
  alertas: { titulo: "Alertas", Icono: IconAlertas, componente: Alertas, roles: ["Usuario", "Admin"] },
  ordenes: { titulo: "Órdenes", Icono: IconHerramienta, componente: Ordenes, roles: ["Admin"] },
  simulador: { titulo: "Simulador", Icono: IconSimulador, componente: Simulador, roles: ["Admin"] },
  rabbit: { titulo: "RabbitMQ", Icono: IconRabbit, componente: Rabbit, roles: ["Admin"] },
};

function iniciales(nombre) {
  return (nombre ?? "?")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");
}

function Contenido() {
  const { instance, accounts } = useMsal();
  const { roles, esAdmin, cargando } = useRoles();
  const [pagina, setPagina] = useState(null);

  const cuenta = accounts[0];
  const rolEfectivo = esAdmin ? "Admin" : "Usuario";
  const visibles = Object.entries(PAGINAS).filter(([, p]) =>
    p.roles.includes(rolEfectivo)
  );

  useEffect(() => {
    if (cargando) return;
    const permitidas = visibles.map(([clave]) => clave);
    if (!pagina || !permitidas.includes(pagina)) {
      setPagina(esAdmin ? "ordenes" : "misOrdenes");
    }
  }, [cargando, esAdmin]); // eslint-disable-line react-hooks/exhaustive-deps

  const Pagina = pagina && PAGINAS[pagina] ? PAGINAS[pagina].componente : null;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-logo">
            <IconLogo size={20} />
          </span>
          <span>Mantenimiento predictivo</span>
        </div>

        <nav className="nav">
          {visibles.map(([clave, { titulo, Icono }]) => (
            <button
              key={clave}
              className={`nav-item ${pagina === clave ? "activa" : ""}`}
              onClick={() => setPagina(clave)}
            >
              <span className="nav-icon">
                <Icono size={20} />
              </span>
              {titulo}
            </button>
          ))}
        </nav>

        <div className="sidebar-user">
          <div className="user-box">
            <span className="avatar">{iniciales(cuenta?.name)}</span>
            <div className="user-info">
              <div className="user-name">{cuenta?.name}</div>
              <div className="user-role">
                {cargando ? "cargando..." : roles.join(", ") || "sin rol"}
              </div>
            </div>
          </div>
          <button className="btn-salir" onClick={() => instance.logoutRedirect()}>
            Salir
          </button>
        </div>
      </aside>

      <main className="main">
        <div className="content">
          {cargando ? <p>Cargando...</p> : Pagina && <Pagina />}
        </div>
      </main>
    </div>
  );
}

function Login() {
  const { instance } = useMsal();

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="brand-logo">
          <IconLogo size={30} />
        </div>
        <h1>Mantenimiento predictivo</h1>
        <p>Monitoreo de equipos, alertas y órdenes de trabajo</p>

        <button
          className="btn-ms"
          onClick={() => instance.loginRedirect(loginRequest)}
        >
          <svg width="20" height="20" viewBox="0 0 21 21" aria-hidden="true">
            <rect x="1" y="1" width="9" height="9" fill="#f25022" />
            <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
            <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
            <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
          </svg>
          Iniciar sesión con Microsoft
        </button>

        <div className="login-foot">
          Acceso con tu cuenta corporativa de Microsoft Entra ID
        </div>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <>
      <UnauthenticatedTemplate>
        <Login />
      </UnauthenticatedTemplate>

      <AuthenticatedTemplate>
        <Contenido />
      </AuthenticatedTemplate>
    </>
  );
}