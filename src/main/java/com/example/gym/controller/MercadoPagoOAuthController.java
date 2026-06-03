package com.example.gym.controller;

import com.example.gym.service.MercadoPagoCredentialService;
import com.example.gym.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MercadoPagoOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoOAuthController.class);

    @Autowired
    private MercadoPagoCredentialService mpCredentials;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${mercadopago.client-id:}")
    private String clientId;

    @Value("${mercadopago.client-secret:}")
    private String clientSecret;

    @Value("${mercadopago.oauth-redirect-uri:http://localhost:8081/api/mp/oauth/callback}")
    private String redirectUri;

    @Value("${mercadopago.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Estado de conexión MP del tenant actual. */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/api/payments/mercadopago")
    public ResponseEntity<?> getConnectionStatus() {
        String tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(mpCredentials.getConnectionInfo(tenantId));
    }

    /** Genera la URL de autorización de MP para iniciar el flujo OAuth. */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/api/payments/mercadopago/oauth-start")
    public ResponseEntity<?> oauthStart() {
        if (clientId == null || clientId.isBlank()) {
            logger.error("[MP OAuth] MP_CLIENT_ID no está configurado");
            return ResponseEntity.badRequest().body("MP_CLIENT_ID no está configurado en el servidor");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            logger.error("[MP OAuth] MP_CLIENT_SECRET no está configurado");
            return ResponseEntity.badRequest().body("MP_CLIENT_SECRET no está configurado en el servidor");
        }
        String tenantId = TenantContext.getCurrentTenant();
        // state = base64(tenantId) — para recuperar el tenant en el callback público
        String state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tenantId.getBytes(StandardCharsets.UTF_8));
        String authUrl = "https://auth.mercadopago.com.ar/authorization"
                + "?client_id="     + enc(clientId)
                + "&response_type=code"
                + "&platform_id=mp"
                + "&state="         + enc(state)
                + "&redirect_uri="  + enc(redirectUri)
                + "&scope=read,write,offline_access";
        logger.info("[MP OAuth] Iniciando OAuth para tenant={} redirectUri={}", tenantId, redirectUri);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * Callback público — MP redirige aquí tras la autorización.
     * Intercambia el code por tokens y los guarda para el tenant.
     * Luego redirige al frontend con ?mp_status=success|error.
     */
    @GetMapping("/api/mp/oauth/callback")
    public void oauthCallback(
            @RequestParam(value = "code",  required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        String base        = frontendUrl.replaceAll("/+$", "");
        String successUrl  = base + "/owner/mercadopago?mp_status=success";
        String errorUrl    = base + "/owner/mercadopago?mp_status=error";

        if (error != null || code == null || state == null) {
            logger.warn("[MP OAuth] Callback con error o parámetros faltantes: error={}", error);
            response.sendRedirect(errorUrl);
            return;
        }

        String tenantId;
        try {
            tenantId = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("[MP OAuth] state inválido: {}", state);
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> body = new HashMap<>();
            body.put("client_id",     clientId);
            body.put("client_secret", clientSecret);
            body.put("code",          code);
            body.put("grant_type",    "authorization_code");
            body.put("redirect_uri",  redirectUri);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/oauth/token"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                Map<String, Object> token = mapper.readValue(resp.body(), new TypeReference<>() {});
                String accessToken  = String.valueOf(token.get("access_token"));
                String refreshToken = String.valueOf(token.getOrDefault("refresh_token", ""));
                String userId       = String.valueOf(token.get("user_id"));
                Object expiresIn    = token.get("expires_in");
                String expiresAt    = expiresIn != null
                        ? Instant.now().plusSeconds(Long.parseLong(String.valueOf(expiresIn))).toString()
                        : null;

                // El callback es público (sin X-Tenant-ID), así que el filtro de tenant
                // puede estar apuntando al tenant default. Lo corregimos antes de guardar.
                overrideTenantContext(tenantId);
                mpCredentials.saveOAuthTokens(tenantId, accessToken, refreshToken, userId, expiresAt);
                logger.info("[MP OAuth] Tokens guardados para tenant={} userId={}", tenantId, userId);
                response.sendRedirect(successUrl);
            } else {
                logger.error("[MP OAuth] Error al intercambiar code: status={} body={}",
                        resp.statusCode(), resp.body());
                response.sendRedirect(errorUrl);
            }
        } catch (Exception e) {
            logger.error("[MP OAuth] Excepción: {}", e.getMessage(), e);
            response.sendRedirect(errorUrl);
        }
    }

    /** Desconecta la cuenta MP del tenant actual. */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/payments/mercadopago/unlink")
    public ResponseEntity<?> unlink() {
        String tenantId = TenantContext.getCurrentTenant();
        mpCredentials.clearTokens(tenantId);
        logger.info("[MP OAuth] Credenciales borradas para tenant={}", tenantId);
        return ResponseEntity.ok(Map.of("message", "Cuenta desconectada correctamente"));
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * El callback OAuth no lleva X-Tenant-ID, así que TenantRequestFilter puede haber
     * activado el tenant default. Corregimos el contexto y el filtro de Hibernate al
     * tenant real (extraído del state) antes de persistir los tokens.
     */
    private void overrideTenantContext(String tenantId) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") != null) {
                session.disableFilter("tenantFilter");
            }
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } catch (Exception ignored) {}
    }
}
