package com.example.gym.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Inyecta el tenant_id en entidades antes de persistir/actualizar.
 */
public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void applyTenant(Object entity) {
        if (!(entity instanceof TenantScoped scoped)) {
            return;
        }
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null && !tenantId.isBlank()) {
            scoped.setTenantId(tenantId);
        }
    }
}
