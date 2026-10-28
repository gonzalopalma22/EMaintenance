package com.mantenimiento.ordenes_trabajo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.mantenimiento.ordenes_trabajo.dto.TecnicoDto;


@Service
public class TecnicoService {

    private static final Logger log = LoggerFactory.getLogger(TecnicoService.class);

    private static final String LOGIN = "https://login.microsoftonline.com/";
    private static final String GRAPH = "https://graph.microsoft.com/v1.0";
    private static final String ROL_TECNICO = "Usuario";
    private static final Duration CACHE = Duration.ofSeconds(60);
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {};

    private final RestClient http = RestClient.create();
    private final String tenantId;
    private final String apiClientId;
    private final String secreto;

    private String token;
    private Instant tokenExpira = Instant.EPOCH;
    private List<TecnicoDto> cache = List.of();
    private Instant cacheHasta = Instant.EPOCH;

    public TecnicoService(@Value("${azure.tenant-id}") String tenantId,
                          @Value("${azure.api-client-id}") String apiClientId,
                          @Value("${azure.client-secret:}") String secreto) {
        this.tenantId = tenantId;
        this.apiClientId = apiClientId;
        this.secreto = secreto;
    }

    public synchronized List<TecnicoDto> listar() {
        if (Instant.now().isBefore(cacheHasta)) {
            return cache;
        }
        if (secreto == null || secreto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falta configurar AZURE_API_CLIENT_SECRET en ordenes-trabajo");
        }
        try {
            cache = consultarGraph();
            cacheHasta = Instant.now().plus(CACHE);
            return cache;
        } catch (RestClientException e) {
            log.error("Error consultando Microsoft Graph", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar la lista de técnicos en Microsoft Entra ID");
        }
    }

    private List<TecnicoDto> consultarGraph() {
        Map<String, Object> sp = graphGet(
                "/servicePrincipals?$filter=appId eq '{id}'&$select=id,appRoles", apiClientId);
        List<Map<String, Object>> principales = lista(sp.get("value"));
        if (principales.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se encontró la aplicación de la API en Entra ID");
        }
        String spId = String.valueOf(principales.get(0).get("id"));

        String rolId = lista(principales.get(0).get("appRoles")).stream()
                .filter(r -> ROL_TECNICO.equals(r.get("value")))
                .map(r -> String.valueOf(r.get("id")))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "La aplicación no define el rol " + ROL_TECNICO));

      
        Map<String, Object> asignaciones = graphGet(
                "/servicePrincipals/{id}/appRoleAssignedTo?$top=999", spId);

        List<TecnicoDto> tecnicos = new ArrayList<>();
        for (Map<String, Object> a : lista(asignaciones.get("value"))) {
            if (!rolId.equals(String.valueOf(a.get("appRoleId")))
                    || !"User".equals(a.get("principalType"))) {
                continue;
            }
            
            Map<String, Object> u = graphGet(
                    "/users/{id}?$select=displayName,userPrincipalName,mail",
                    String.valueOf(a.get("principalId")));
            tecnicos.add(new TecnicoDto(
                    String.valueOf(u.getOrDefault("displayName", "")),
                    correoDeLogin(u)));
        }
        tecnicos.sort(Comparator.comparing(TecnicoDto::nombre, String.CASE_INSENSITIVE_ORDER));
        return tecnicos;
    }

   
    private static String correoDeLogin(Map<String, Object> u) {
        String upn = String.valueOf(u.get("userPrincipalName"));
        Object mail = u.get("mail");
       
        if (upn.contains("#EXT#") && mail != null && !String.valueOf(mail).isBlank()) {
            return String.valueOf(mail);
        }
        return upn;
    }

    private synchronized String token() {
        if (token != null && Instant.now().isBefore(tokenExpira)) {
            return token;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", apiClientId);
        form.add("client_secret", secreto);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");

        Map<String, Object> r = http.post()
                .uri(LOGIN + tenantId + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(MAPA);

        token = String.valueOf(r.get("access_token"));
        long segundos = r.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        tokenExpira = Instant.now().plusSeconds(Math.max(60, segundos - 120));
        return token;
    }

    private Map<String, Object> graphGet(String ruta, Object... variables) {
        return http.get()
                .uri(GRAPH + ruta, variables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .body(MAPA);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object valor) {
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }
}