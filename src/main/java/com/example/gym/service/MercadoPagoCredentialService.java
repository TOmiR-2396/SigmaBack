package com.example.gym.service;

import com.example.gym.tenant.TenantContext;
import com.example.gym.tenant.TenantSwitch;
import com.example.gym.tenant.TenantSwitchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class MercadoPagoCredentialService {

    private static final String KEY_ACCESS_TOKEN      = "MP_ACCESS_TOKEN";
    private static final String KEY_WEBHOOK_SECRET    = "MP_WEBHOOK_SECRET";
    private static final String KEY_COMMISSION_PERCENT = "MP_COMMISSION_PERCENT";
    private static final String KEY_REFRESH_TOKEN     = "MP_REFRESH_TOKEN";
    private static final String KEY_USER_ID           = "MP_USER_ID";
    private static final String KEY_TOKEN_EXPIRES_AT  = "MP_TOKEN_EXPIRES_AT";

    /** Public key de GESTIGYM (el integrador). Nunca cambia por tenant. */
    @Value("${mercadopago.integrator-public-key:}")
    private String integratorPublicKey;

    @Value("${mercadopago.access-token:}")
    private String defaultAccessToken;

    @Value("${mercadopago.webhook-secret:}")
    private String defaultWebhookSecret;

    @Value("${mercadopago.commission-percent:5.0}")
    private double defaultCommissionPercent;

    @Autowired
    private TenantSwitchRepository switchRepository;

    /**
     * Public key de GESTIGYM (el integrador/plataforma).
     * Se usa en el frontend para inicializar el SDK de MP.
     * Es siempre la misma independientemente del tenant.
     */
    public String getIntegratorPublicKey() {
        return integratorPublicKey;
    }

    /** Access token del seller (tenant actual). DB primero, @Value como fallback. */
    public String getAccessToken() {
        return resolve(KEY_ACCESS_TOKEN, defaultAccessToken);
    }

    public String getWebhookSecret() {
        return resolve(KEY_WEBHOOK_SECRET, defaultWebhookSecret);
    }

    /** Para el webhook: resolve por tenantId explícito (sin TenantContext). */
    public String getAccessTokenForTenant(String tenantId) {
        return resolveForTenant(tenantId, KEY_ACCESS_TOKEN, defaultAccessToken);
    }

    /**
     * Comisión de gestigym para el tenant actual (%).
     * Ej: 5.0 → 5%. Se puede configurar por tenant en DB.
     */
    public double getCommissionPercent() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) return defaultCommissionPercent;
        String stored = resolveForTenant(tenantId, KEY_COMMISSION_PERCENT, null);
        if (stored == null) return defaultCommissionPercent;
        try { return Double.parseDouble(stored); } catch (NumberFormatException e) { return defaultCommissionPercent; }
    }

    /**
     * Calcula el monto de la comisión en ARS para un precio dado.
     * marketplace_fee / application_fee = redondeo al peso más cercano.
     */
    public double calculateFee(double price) {
        return Math.round(price * getCommissionPercent() / 100.0);
    }

    /** Devuelve las credenciales del tenant para mostrar en UI (access token enmascarado). */
    public Map<String, String> getForDisplay(String tenantId) {
        String at = resolveForTenant(tenantId, KEY_ACCESS_TOKEN, null);
        String ws = resolveForTenant(tenantId, KEY_WEBHOOK_SECRET, null);
        String cp = resolveForTenant(tenantId, KEY_COMMISSION_PERCENT, String.valueOf(defaultCommissionPercent));
        return Map.of(
            "accessToken",       at != null ? maskToken(at) : "",
            "webhookSecret",     ws != null ? ws : "",
            "commissionPercent", cp != null ? cp : String.valueOf(defaultCommissionPercent)
        );
    }

    /** Devuelve true si el tenant tiene un access token guardado (OAuth conectado). */
    public boolean isConnected(String tenantId) {
        String token = resolveForTenant(tenantId, KEY_ACCESS_TOKEN, null);
        return token != null && !token.isBlank();
    }

    /** Info de conexión para mostrar en la UI del owner. */
    public Map<String, Object> getConnectionInfo(String tenantId) {
        String token = resolveForTenant(tenantId, KEY_ACCESS_TOKEN, null);
        boolean connected = token != null && !token.isBlank();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("connected", connected);
        if (!connected) return result;
        String userId    = resolveForTenant(tenantId, KEY_USER_ID, "");
        String expiresAt = resolveForTenant(tenantId, KEY_TOKEN_EXPIRES_AT, "");
        result.put("mpUserId",    userId    != null ? userId    : "");
        result.put("accessToken", maskToken(token));
        result.put("expiresAt",   expiresAt != null ? expiresAt : "");
        return result;
    }

    /** Guarda tokens obtenidos vía OAuth. */
    @Transactional
    public void saveOAuthTokens(String tenantId, String accessToken, String refreshToken,
                                String userId, String expiresAt) {
        upsert(tenantId, KEY_ACCESS_TOKEN, accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) upsert(tenantId, KEY_REFRESH_TOKEN, refreshToken);
        if (userId    != null && !userId.isBlank())          upsert(tenantId, KEY_USER_ID,          userId);
        if (expiresAt != null && !expiresAt.isBlank())       upsert(tenantId, KEY_TOKEN_EXPIRES_AT, expiresAt);
    }

    /** Borra las credenciales OAuth del tenant (desconecta). */
    @Transactional
    public void clearTokens(String tenantId) {
        clear(tenantId, KEY_ACCESS_TOKEN);
        clear(tenantId, KEY_REFRESH_TOKEN);
        clear(tenantId, KEY_USER_ID);
        clear(tenantId, KEY_TOKEN_EXPIRES_AT);
    }

    @Transactional
    public void save(String tenantId, String accessToken, String publicKey, String webhookSecret) {
        upsert(tenantId, KEY_ACCESS_TOKEN,   accessToken);
        upsert(tenantId, KEY_WEBHOOK_SECRET, webhookSecret);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolve(String key, String fallback) {
        String tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? resolveForTenant(tenantId, key, fallback) : fallback;
    }

    private String resolveForTenant(String tenantId, String key, String fallback) {
        return switchRepository.findByTenantIdAndKey(tenantId, key)
                .map(TenantSwitch::getPayload)
                .filter(p -> p != null && !p.isBlank())
                .orElse(fallback);
    }

    private void upsert(String tenantId, String key, String value) {
        if (value == null) return;
        TenantSwitch sw = switchRepository.findByTenantIdAndKey(tenantId, key)
                .orElseGet(() -> {
                    TenantSwitch n = TenantSwitch.builder().key(key).build();
                    n.setTenantId(tenantId);
                    return n;
                });
        sw.setPayload(value.trim());
        sw.setEnabled(true);
        switchRepository.saveAndFlush(sw);
    }

    private void clear(String tenantId, String key) {
        switchRepository.findByTenantIdAndKey(tenantId, key).ifPresent(sw -> {
            sw.setPayload("");
            sw.setEnabled(false);
            switchRepository.save(sw);
        });
    }

    private String maskToken(String token) {
        if (token.length() <= 14) return "****";
        return token.substring(0, 10) + "..." + token.substring(token.length() - 4);
    }
}
