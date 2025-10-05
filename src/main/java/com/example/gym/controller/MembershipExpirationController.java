package com.example.gym.controller;

import com.example.gym.service.MembershipExpirationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/membership-expiration")
public class MembershipExpirationController {

    @Autowired
    private MembershipExpirationService expirationService;

    /**
     * Obtener estadísticas de membresías próximas a vencer
     * Solo accesible para usuarios OWNER
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> getExpirationStats() {
        try {
            String stats = expirationService.getMembershipExpirationStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error obteniendo estadísticas: " + e.getMessage());
        }
    }

    /**
     * Forzar la ejecución manual del proceso de verificación de membresías vencidas
     * Solo accesible para usuarios OWNER
     */
    @PostMapping("/force-check")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> forceExpirationCheck() {
        try {
            expirationService.forceExpirationCheck();
            return ResponseEntity.ok("Proceso de verificación de membresías vencidas ejecutado correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error ejecutando verificación: " + e.getMessage());
        }
    }
}