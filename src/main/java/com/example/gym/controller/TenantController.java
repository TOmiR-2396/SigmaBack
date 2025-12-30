package com.example.gym.controller;

import com.example.gym.dto.FeatureFlagDTO;
import com.example.gym.tenant.TenantContext;
import com.example.gym.tenant.TenantSwitchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de tenant: info, features, configuración.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantSwitchRepository switchRepository;

    public TenantController(TenantSwitchRepository switchRepository) {
        this.switchRepository = switchRepository;
    }

    /**
     * GET /api/tenants/me/features
     * Lista todos los feature flags habilitados del tenant actual.
     * Solo lectura; el CRUD viene después con permisos OWNER.
     */
    @GetMapping("/me/features")
    public ResponseEntity<?> getMyFeatures(Authentication authentication) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.badRequest().body("No tenant in context");
        }

        List<FeatureFlagDTO> features = switchRepository.findAll().stream()
            .filter(sw -> tenantId.equals(sw.getTenantId()))
            .map(sw -> new FeatureFlagDTO(sw.getKey(), sw.isEnabled(), sw.getPayload()))
            .toList();

        return ResponseEntity.ok(features);
    }

    /**
     * GET /api/tenants/me
     * Info del tenant actual.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getTenantInfo(Authentication authentication) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.badRequest().body("No tenant in context");
        }

        return ResponseEntity.ok(Map.of(
            "tenantId", tenantId,
            "user", authentication != null ? authentication.getName() : "anonymous"
        ));
    }
}
