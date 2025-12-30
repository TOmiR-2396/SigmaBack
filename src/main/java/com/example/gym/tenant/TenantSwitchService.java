package com.example.gym.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantSwitchService {

    private final TenantSwitchRepository repository;

    @Value("${multitenancy.switches.cache-seconds:60}")
    private long cacheSeconds;

    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();

    public TenantSwitchService(TenantSwitchRepository repository) {
        this.repository = repository;
    }

    public boolean isEnabled(String key, boolean defaultValue) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            return defaultValue;
        }
        String cacheKey = tenantId + "::" + key;
        CacheItem item = cache.get(cacheKey);
        if (item != null && item.expiresAt > Instant.now().getEpochSecond()) {
            return item.enabled;
        }
        Optional<TenantSwitch> sw = repository.findByTenantIdAndKey(tenantId, key);
        boolean enabled = sw.map(TenantSwitch::isEnabled).orElse(defaultValue);
        cache.put(cacheKey, new CacheItem(enabled, Instant.now().getEpochSecond() + cacheSeconds));
        return enabled;
    }

    public Optional<String> getPayload(String key) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTenantIdAndKey(tenantId, key).map(TenantSwitch::getPayload);
    }

    private record CacheItem(boolean enabled, long expiresAt) {}
}
