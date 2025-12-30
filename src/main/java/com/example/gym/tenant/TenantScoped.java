package com.example.gym.tenant;

public interface TenantScoped {
    String getTenantId();
    void setTenantId(String tenantId);
}
