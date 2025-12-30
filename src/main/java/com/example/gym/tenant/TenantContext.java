package com.example.gym.tenant;

/**
 * Mantiene el tenant actual a nivel de hilo.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
