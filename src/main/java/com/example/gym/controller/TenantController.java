package com.example.gym.controller;

import com.example.gym.dto.FeatureFlagDTO;
import com.example.gym.service.MercadoPagoCredentialService;
import com.example.gym.tenant.TenantContext;
import com.example.gym.tenant.TenantSwitchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de tenant: info, features, configuración.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantSwitchRepository switchRepository;

    @Autowired
    private MercadoPagoCredentialService mpCredentialService;

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
     * GET /api/admin/tenants/{tenantId}/mercadopago
     * Devuelve las credenciales de MP del tenant (access token enmascarado).
     */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @GetMapping("/admin/tenants/{tenantId}/mercadopago")
    public ResponseEntity<?> getMpCredentials(@PathVariable String tenantId) {
        String current = TenantContext.getCurrentTenant();
        if (current != null && !current.equals(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado para este tenant");
        }
        return ResponseEntity.ok(mpCredentialService.getForDisplay(tenantId));
    }

    /**
     * PUT /api/admin/tenants/{tenantId}/mercadopago
     * Guarda las credenciales de MP del tenant.
     */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PutMapping("/admin/tenants/{tenantId}/mercadopago")
    public ResponseEntity<?> saveMpCredentials(
            @PathVariable String tenantId,
            @RequestBody Map<String, String> body) {
        String current = TenantContext.getCurrentTenant();
        if (current != null && !current.equals(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado para este tenant");
        }
        if (body.get("accessToken") == null || body.get("accessToken").isBlank()) {
            return ResponseEntity.badRequest().body("accessToken es obligatorio");
        }
        mpCredentialService.save(tenantId, body.get("accessToken"), body.get("publicKey"), body.get("webhookSecret"));
        return ResponseEntity.ok(Map.of("message", "Credenciales guardadas correctamente"));
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
