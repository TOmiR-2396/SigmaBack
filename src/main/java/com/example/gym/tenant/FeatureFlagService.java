package com.example.gym.tenant;

import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Wrapper conveniente para consultas de feature flags por tenant.
 * Abstrae la lógica de TenantSwitchService para controladores y servicios.
 */
@Service
public class FeatureFlagService {

    private final TenantSwitchService switchService;

    public FeatureFlagService(TenantSwitchService switchService) {
        this.switchService = switchService;
    }

    /**
     * Consulta si una feature está habilitada para el tenant actual.
     * Lanza excepción si no hay tenant resuelto.
     */
    public boolean isEnabled(String featureKey) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("No tenant context available");
        }
        return switchService.isEnabled(featureKey, false);
    }

    /**
     * Consulta con valor por defecto si la feature no existe.
     */
    public boolean isEnabled(String featureKey, boolean defaultValue) {
        return switchService.isEnabled(featureKey, defaultValue);
    }

    /**
     * Obtiene el payload JSON asociado a una feature.
     */
    public Optional<String> getPayload(String featureKey) {
        return switchService.getPayload(featureKey);
    }

    /**
     * Guard-clause: lanza excepción si la feature NO está habilitada.
     */
    public void requireEnabled(String featureKey, String errorMessage) {
        if (!isEnabled(featureKey)) {
            throw new FeatureNotEnabledException(errorMessage);
        }
    }

    public static class FeatureNotEnabledException extends RuntimeException {
        public FeatureNotEnabledException(String message) {
            super(message);
        }
    }
}
