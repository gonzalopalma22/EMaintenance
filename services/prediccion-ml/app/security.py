import logging
import os
import time

import httpx
from dotenv import load_dotenv
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2AuthorizationCodeBearer
from jose import JWTError, jwt

load_dotenv()

log = logging.getLogger("prediccion-ml.security")

TENANT_ID = os.getenv("AZURE_TENANT_ID")
CLIENT_ID = os.getenv("AZURE_API_CLIENT_ID")

# Falla al arrancar si faltan, en vez de armar URLs con "None"
if not TENANT_ID or not CLIENT_ID:
    raise RuntimeError("Faltan AZURE_TENANT_ID o AZURE_API_CLIENT_ID")

JWKS_URL = f"https://login.microsoftonline.com/{TENANT_ID}/discovery/v2.0/keys"
ISSUER = f"https://login.microsoftonline.com/{TENANT_ID}/v2.0"

oauth2_scheme = OAuth2AuthorizationCodeBearer(
    authorizationUrl=f"https://login.microsoftonline.com/{TENANT_ID}/oauth2/v2.0/authorize",
    tokenUrl=f"https://login.microsoftonline.com/{TENANT_ID}/oauth2/v2.0/token",
)

# Caché de llaves públicas: se renuevan cada hora, o antes si llega un kid
# desconocido (Microsoft rota llaves), con un mínimo de 60 s entre descargas.
JWKS_TTL = 3600
JWKS_MIN_REFRESH = 60
_cache = {"keys": [], "fetched_at": 0.0}


async def _descargar_llaves() -> None:
    async with httpx.AsyncClient(timeout=5) as client:
        r = await client.get(JWKS_URL)
        r.raise_for_status()
        _cache["keys"] = r.json()["keys"]
        _cache["fetched_at"] = time.time()


async def _buscar_llave(kid: str):
    ahora = time.time()
    if not _cache["keys"] or ahora - _cache["fetched_at"] > JWKS_TTL:
        await _descargar_llaves()

    for k in _cache["keys"]:
        if k["kid"] == kid:
            return k

    # kid desconocido: puede haber rotación; reintenta una vez si pasó el mínimo
    if time.time() - _cache["fetched_at"] > JWKS_MIN_REFRESH:
        await _descargar_llaves()
        for k in _cache["keys"]:
            if k["kid"] == kid:
                return k
    return None


async def verify_token(token: str = Depends(oauth2_scheme)) -> dict:
    """Valida firma, emisor, audiencia y expiración. Devuelve el payload."""
    try:
        header = jwt.get_unverified_header(token)
        llave = await _buscar_llave(header.get("kid"))
        if llave is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Firma del token no encontrada",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return jwt.decode(
            token,
            llave,
            algorithms=["RS256"],
            audience=CLIENT_ID,
            issuer=ISSUER,
        )
    except HTTPException:
        raise
    except JWTError as e:
        log.warning("Token rechazado: %s", e)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido o expirado",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except httpx.HTTPError as e:
        log.error("No se pudieron obtener las llaves de Microsoft: %s", e)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="No se pudo validar el token en este momento",
        )


def require_roles(*permitidos: str):
    """Dependencia que exige que el token tenga alguno de los roles dados
    (la comparación no distingue mayúsculas de minúsculas)."""
    async def checker(payload: dict = Depends(verify_token)) -> dict:
        roles = payload.get("roles", [])
        if isinstance(roles, str):
            roles = [roles]

        roles_token = {r.lower() for r in roles}
        exigidos = {p.lower() for p in permitidos}

        if not roles_token & exigidos:
            log.warning(
                "403 | usuario=%s | roles en token=%r | se exigía uno de=%s",
                payload.get("preferred_username"), roles, permitidos,
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Rol insuficiente",
            )
        return payload
    return checker