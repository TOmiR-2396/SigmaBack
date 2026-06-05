package com.example.gym.controller;

import com.example.gym.dto.GoogleSheetsConfigRequest;
import com.example.gym.dto.GoogleSheetsPreviewResponse;
import com.example.gym.service.GoogleSheetsCredentialService;
import com.example.gym.service.GoogleSheetsService;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
public class GoogleSheetsController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsController.class);

    @Autowired
    private GoogleSheetsCredentialService credentialService;

    @Autowired
    private GoogleSheetsService sheetsService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Value("${google.redirect-uri:http://localhost:8081/api/google-sheets/oauth/callback}")
    private String redirectUri;

    @Value("${google.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== STATUS ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/api/google-sheets/status")
    public ResponseEntity<?> getStatus() {
        String tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(credentialService.getConnectionInfo(tenantId));
    }

    // ==================== OAUTH START ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/api/google-sheets/oauth-start")
    public ResponseEntity<?> oauthStart() {
        if (clientId == null || clientId.isBlank()) {
            logger.error("[Google Sheets] GOOGLE_CLIENT_ID no está configurado");
            return ResponseEntity.badRequest().body("GOOGLE_CLIENT_ID no está configurado en el servidor");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            logger.error("[Google Sheets] GOOGLE_CLIENT_SECRET no está configurado");
            return ResponseEntity.badRequest().body("GOOGLE_CLIENT_SECRET no está configurado en el servidor");
        }

        String tenantId = TenantContext.getCurrentTenant();
        String state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tenantId.getBytes(StandardCharsets.UTF_8));

        String scope = "https://www.googleapis.com/auth/spreadsheets";
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc(scope)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + enc(state);

        logger.info("[Google Sheets] Iniciando OAuth para tenant={}", tenantId);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    // ==================== OAUTH CALLBACK ====================

    @GetMapping("/api/google-sheets/oauth/callback")
    public void oauthCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        String base = frontendUrl.replaceAll("/+$", "");
        String successUrl = base + "/owner/google-sheets?gs_status=success";
        String errorUrl = base + "/owner/google-sheets?gs_status=error";

        if (error != null || code == null || state == null) {
            logger.warn("[Google Sheets] Callback con error o parámetros faltantes: error={}", error);
            response.sendRedirect(errorUrl);
            return;
        }

        String tenantId;
        try {
            tenantId = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("[Google Sheets] state inválido: {}", state);
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            logger.info("[Google Sheets] Intercambiando code por tokens — tenant={}", tenantId);

            String body = "grant_type=authorization_code"
                    + "&code=" + enc(code)
                    + "&redirect_uri=" + enc(redirectUri)
                    + "&client_id=" + enc(clientId)
                    + "&client_secret=" + enc(clientSecret);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            logger.info("[Google Sheets] Respuesta de token — status={}", resp.statusCode());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                Map<String, Object> token = mapper.readValue(resp.body(), new TypeReference<>() {});
                String accessToken = String.valueOf(token.get("access_token"));
                String refreshToken = token.containsKey("refresh_token")
                        ? String.valueOf(token.get("refresh_token")) : null;

                logger.info("[Google Sheets] Access token recibido (len={})", accessToken.length());

                overrideTenantContext(tenantId);
                credentialService.saveOAuthTokens(tenantId, accessToken, refreshToken);
                logger.info("[Google Sheets] Tokens guardados para tenant={}", tenantId);
                response.sendRedirect(successUrl);
            } else {
                logger.error("[Google Sheets] Error al intercambiar code: status={} body={}",
                        resp.statusCode(), resp.body());
                String detail = "";
                try {
                    Map<String, Object> err = mapper.readValue(resp.body(), new TypeReference<>() {});
                    detail = String.valueOf(err.getOrDefault("error_description",
                            err.getOrDefault("error", "")));
                } catch (Exception ignored) {}
                response.sendRedirect(errorUrl + (detail.isBlank() ? "" : "&detail=" + enc(detail)));
            }
        } catch (Exception e) {
            logger.error("[Google Sheets] Excepción en callback: {}", e.getMessage(), e);
            response.sendRedirect(errorUrl);
        }
    }

    // ==================== DISCONNECT ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/google-sheets/disconnect")
    public ResponseEntity<?> disconnect() {
        String tenantId = TenantContext.getCurrentTenant();
        credentialService.clearTokens(tenantId);
        logger.info("[Google Sheets] Credenciales borradas para tenant={}", tenantId);
        return ResponseEntity.ok(Map.of("message", "Cuenta de Google desconectada correctamente"));
    }

    // ==================== CONFIGURE ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/google-sheets/configure")
    public ResponseEntity<?> configure(@RequestBody GoogleSheetsConfigRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        String paymentsSheetId = extractSheetId(request.getPaymentsSheetUrl());
        String plansSheetId = extractSheetId(request.getPlansSheetUrl());

        StringBuilder features = new StringBuilder();
        if (request.isSyncPayments()) features.append("payments,");
        if (request.isSyncPlans()) features.append("plans,");
        String enabledFeatures = features.toString();
        if (enabledFeatures.endsWith(",")) {
            enabledFeatures = enabledFeatures.substring(0, enabledFeatures.length() - 1);
        }

        credentialService.saveConfiguration(tenantId, paymentsSheetId, plansSheetId, enabledFeatures);
        logger.info("[Google Sheets] Configuración guardada para tenant={} features={}", tenantId, enabledFeatures);
        return ResponseEntity.ok(Map.of("message", "Configuración guardada correctamente"));
    }

    // ==================== PREVIEW ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/api/google-sheets/preview/payments")
    public ResponseEntity<?> previewPayments() {
        String tenantId = TenantContext.getCurrentTenant();
        String sheetId = credentialService.getPaymentsSheetId();
        if (sheetId == null || sheetId.isBlank()) {
            return ResponseEntity.badRequest().body("No está configurada la hoja de pagos");
        }
        try {
            GoogleSheetsPreviewResponse preview = sheetsService.previewPayments(sheetId);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            logger.error("[Google Sheets] Error en preview payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/api/google-sheets/preview/plans")
    public ResponseEntity<?> previewPlans() {
        String tenantId = TenantContext.getCurrentTenant();
        String sheetId = credentialService.getPlansSheetId();
        if (sheetId == null || sheetId.isBlank()) {
            return ResponseEntity.badRequest().body("No está configurada la hoja de planes");
        }
        try {
            GoogleSheetsPreviewResponse preview = sheetsService.previewPlans(sheetId);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            logger.error("[Google Sheets] Error en preview plans: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== SYNC ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/google-sheets/sync/payments")
    public ResponseEntity<?> syncPayments() {
        String tenantId = TenantContext.getCurrentTenant();
        String sheetId = credentialService.getPaymentsSheetId();
        if (sheetId == null || sheetId.isBlank()) {
            return ResponseEntity.badRequest().body("No está configurada la hoja de pagos");
        }
        try {
            Map<String, Object> result = sheetsService.syncPaymentsToSheet(sheetId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[Google Sheets] Error sync payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/google-sheets/sync/plans")
    public ResponseEntity<?> syncPlans() {
        String tenantId = TenantContext.getCurrentTenant();
        String sheetId = credentialService.getPlansSheetId();
        if (sheetId == null || sheetId.isBlank()) {
            return ResponseEntity.badRequest().body("No está configurada la hoja de planes");
        }
        try {
            Map<String, Object> result = sheetsService.syncPlansToSheet(sheetId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[Google Sheets] Error sync plans: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== IMPORT ====================

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/google-sheets/import/payments")
    public ResponseEntity<?> importPayments() {
        String tenantId = TenantContext.getCurrentTenant();
        String sheetId = credentialService.getPaymentsSheetId();
        if (sheetId == null || sheetId.isBlank()) {
            return ResponseEntity.badRequest().body("No está configurada la hoja de pagos");
        }
        try {
            Map<String, Object> result = sheetsService.importPaymentsFromSheet(sheetId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[Google Sheets] Error import payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping("/api/google-sheets/import/plans")
    public ResponseEntity<?> importPlans() {
        String tenantId = TenantContext.getCurrentTenant();
        String sheetId = credentialService.getPlansSheetId();
        if (sheetId == null || sheetId.isBlank()) {
            return ResponseEntity.badRequest().body("No está configurada la hoja de planes");
        }
        try {
            Map<String, Object> result = sheetsService.importPlansFromSheet(sheetId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[Google Sheets] Error import plans: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== HELPERS ====================

    private String enc(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String extractSheetId(String url) {
        if (url == null || url.isBlank()) return null;
        // Formatos posibles:
        // https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit
        // https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit#gid=0
        if (url.contains("/d/")) {
            String afterD = url.substring(url.indexOf("/d/") + 3);
            int slash = afterD.indexOf("/");
            int query = afterD.indexOf("?");
            int hash = afterD.indexOf("#");
            int end = afterD.length();
            if (slash > 0) end = Math.min(end, slash);
            if (query > 0) end = Math.min(end, query);
            if (hash > 0) end = Math.min(end, hash);
            return afterD.substring(0, end);
        }
        // Si ya es solo el ID
        if (url.matches("[a-zA-Z0-9-_]+")) return url;
        return null;
    }

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
