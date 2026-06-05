package com.example.gym.controller;

import com.example.gym.model.Tenant;
import com.example.gym.repository.TenantRepository;
import com.example.gym.tenant.TenantSwitchRepository;
import com.example.gym.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminTenantController {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantSwitchRepository switchRepository;
    @Autowired private EmailService emailService;

    /** Lista todos los gimnasios */
    @GetMapping
    public ResponseEntity<?> listTenants() {
        return ResponseEntity.ok(tenantRepository.findAll().stream().map(this::toDto).toList());
    }

    /** Registrar nuevo gimnasio */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String tenantId = body.get("tenantId");
        String name     = body.get("name");
        if (tenantId == null || tenantId.isBlank() || name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body("tenantId y name son obligatorios");
        }
        if (tenantRepository.findByTenantId(tenantId).isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe un gimnasio con ese tenantId");
        }
        Tenant t = Tenant.builder()
                .tenantId(tenantId.trim().toLowerCase())
                .name(name.trim())
                .contactEmail(body.get("contactEmail"))
                .contactPhone(body.get("contactPhone"))
                .plan(body.getOrDefault("plan", "BASIC"))
                .notes(body.get("notes"))
                .status("ACTIVE")
                .build();
        return ResponseEntity.ok(toDto(tenantRepository.save(t)));
    }

    /** Detalle de un gimnasio */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return tenantRepository.findById(id)
                .map(t -> ResponseEntity.ok(toDto(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Editar datos de un gimnasio */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tenantRepository.findById(id).map(t -> {
            if (body.containsKey("name"))         t.setName(body.get("name"));
            if (body.containsKey("contactEmail")) t.setContactEmail(body.get("contactEmail"));
            if (body.containsKey("contactPhone")) t.setContactPhone(body.get("contactPhone"));
            if (body.containsKey("plan"))         t.setPlan(body.get("plan"));
            if (body.containsKey("notes"))        t.setNotes(body.get("notes"));
            return ResponseEntity.ok(toDto(tenantRepository.save(t)));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Suspender gimnasio */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return tenantRepository.findById(id).map(t -> {
            t.setStatus("SUSPENDED");
            if (body != null && body.get("notes") != null) t.setNotes(body.get("notes"));
            tenantRepository.save(t);
            return ResponseEntity.ok(Map.of("message", "Gimnasio suspendido"));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Reactivar gimnasio */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        return tenantRepository.findById(id).map(t -> {
            t.setStatus("ACTIVE");
            tenantRepository.save(t);
            return ResponseEntity.ok(Map.of("message", "Gimnasio activado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Cambiar estado */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tenantRepository.findById(id).map(t -> {
            t.setStatus(body.getOrDefault("status", t.getStatus()));
            return ResponseEntity.ok(toDto(tenantRepository.save(t)));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Features del gimnasio */
    @GetMapping("/{tenantId}/features")
    public ResponseEntity<?> getFeatures(@PathVariable String tenantId) {
        var features = switchRepository.findAll().stream()
                .filter(sw -> tenantId.equals(sw.getTenantId()))
                .map(sw -> Map.of("key", sw.getKey(), "enabled", sw.isEnabled(),
                                  "payload", sw.getPayload() != null ? sw.getPayload() : ""))
                .toList();
        return ResponseEntity.ok(features);
    }

    /** Contactar gimnasio por email */
    @PostMapping("/{id}/contact")
    public ResponseEntity<?> contact(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<Tenant> opt = tenantRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Tenant t = opt.get();
        if (t.getContactEmail() == null || t.getContactEmail().isBlank()) {
            return ResponseEntity.badRequest().body("El gimnasio no tiene email de contacto");
        }
        String subject = body.getOrDefault("subject", "Mensaje de GestiGym");
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");
        }
        emailService.sendContactEmail(t.getContactEmail(), t.getName(), subject, message);
        return ResponseEntity.ok(Map.of("message", "Email enviado a " + t.getContactEmail()));
    }

    private Map<String, Object> toDto(Tenant t) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id",           t.getId());
        dto.put("tenantId",     t.getTenantId());
        dto.put("name",         t.getName());
        dto.put("status",       t.getStatus());
        dto.put("contactEmail", t.getContactEmail());
        dto.put("contactPhone", t.getContactPhone());
        dto.put("plan",         t.getPlan());
        dto.put("notes",        t.getNotes());
        dto.put("createdAt",    t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        return dto;
    }
}
