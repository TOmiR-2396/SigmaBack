package com.example.gym.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TenantSwitchRepository extends JpaRepository<TenantSwitch, Long> {
    Optional<TenantSwitch> findByTenantIdAndKey(String tenantId, String key);

    List<TenantSwitch> findByKey(String key);
}
