package com.example.gym.service;

import com.example.gym.tenant.TenantContext;
import com.example.gym.tenant.TenantSwitch;
import com.example.gym.tenant.TenantSwitchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleSheetsCredentialService {

    private static final String KEY_ACCESS_TOKEN     = "GS_ACCESS_TOKEN";
    private static final String KEY_REFRESH_TOKEN    = "GS_REFRESH_TOKEN";
    private static final String KEY_PAYMENTS_SHEET   = "GS_PAYMENTS_SHEET_ID";
    private static final String KEY_PLANS_SHEET      = "GS_PLANS_SHEET_ID";
    private static final String KEY_CONNECTED_AT     = "GS_CONNECTED_AT";
    private static final String KEY_ENABLED_FEATURES = "GS_ENABLED_FEATURES";

    @Autowired
    private TenantSwitchRepository switchRepository;

    // --- Getters ---

    public String getAccessToken() {
        return resolve(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return resolve(KEY_REFRESH_TOKEN, null);
    }

    public String getPaymentsSheetId() {
        return resolve(KEY_PAYMENTS_SHEET, null);
    }

    public String getPlansSheetId() {
        return resolve(KEY_PLANS_SHEET, null);
    }

    public String getEnabledFeatures() {
        return resolve(KEY_ENABLED_FEATURES, "");
    }

    public boolean isConnected(String tenantId) {
        String token = resolveForTenant(tenantId, KEY_ACCESS_TOKEN, null);
        return token != null && !token.isBlank();
    }

    public boolean isConfigured(String tenantId) {
        String payments = resolveForTenant(tenantId, KEY_PAYMENTS_SHEET, null);
        String plans = resolveForTenant(tenantId, KEY_PLANS_SHEET, null);
        String features = resolveForTenant(tenantId, KEY_ENABLED_FEATURES, "");
        return !features.isBlank() && ((features.contains("payments") && payments != null && !payments.isBlank())
                || (features.contains("plans") && plans != null && !plans.isBlank()));
    }

    // --- Status ---

    public Map<String, Object> getConnectionInfo(String tenantId) {
        Map<String, Object> result = new HashMap<>();
        boolean connected = isConnected(tenantId);
        result.put("connected", connected);
        if (!connected) return result;

        result.put("paymentsSheetId", maskId(resolveForTenant(tenantId, KEY_PAYMENTS_SHEET, "")));
        result.put("plansSheetId", maskId(resolveForTenant(tenantId, KEY_PLANS_SHEET, "")));
        result.put("enabledFeatures", resolveForTenant(tenantId, KEY_ENABLED_FEATURES, ""));
        result.put("connectedAt", resolveForTenant(tenantId, KEY_CONNECTED_AT, ""));
        return result;
    }

    // --- Save / Clear ---

    @Transactional
    public void saveOAuthTokens(String tenantId, String accessToken, String refreshToken) {
        upsert(tenantId, KEY_ACCESS_TOKEN, accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            upsert(tenantId, KEY_REFRESH_TOKEN, refreshToken);
        }
        upsert(tenantId, KEY_CONNECTED_AT, Instant.now().toString());
    }

    @Transactional
    public void saveConfiguration(String tenantId, String paymentsSheetId, String plansSheetId, String enabledFeatures) {
        if (paymentsSheetId != null) upsert(tenantId, KEY_PAYMENTS_SHEET, paymentsSheetId);
        if (plansSheetId != null) upsert(tenantId, KEY_PLANS_SHEET, plansSheetId);
        if (enabledFeatures != null) upsert(tenantId, KEY_ENABLED_FEATURES, enabledFeatures);
    }

    @Transactional
    public void clearTokens(String tenantId) {
        clear(tenantId, KEY_ACCESS_TOKEN);
        clear(tenantId, KEY_REFRESH_TOKEN);
        clear(tenantId, KEY_PAYMENTS_SHEET);
        clear(tenantId, KEY_PLANS_SHEET);
        clear(tenantId, KEY_CONNECTED_AT);
        clear(tenantId, KEY_ENABLED_FEATURES);
    }

    @Transactional
    public void updateAccessToken(String tenantId, String newAccessToken) {
        upsert(tenantId, KEY_ACCESS_TOKEN, newAccessToken);
    }

    // --- Helpers ---

    private String resolve(String key) {
        String tenantId = TenantContext.getCurrentTenant();
        return resolveForTenant(tenantId, key, null);
    }

    private String resolve(String key, String fallback) {
        String tenantId = TenantContext.getCurrentTenant();
        return resolveForTenant(tenantId, key, fallback);
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

    private String maskId(String id) {
        if (id == null || id.length() <= 8) return id;
        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }
}
