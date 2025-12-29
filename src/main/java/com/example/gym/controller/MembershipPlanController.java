package com.example.gym.controller;

import com.example.gym.dto.MembershipInfoDTO;
import com.example.gym.dto.MembershipPlanDTO;
import com.example.gym.model.User;
import com.example.gym.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * MembershipPlanController: HTTP endpoints para planes de membresía
 * Usa MembershipService para lógica de negocio
 */
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipPlanController {

    private final MembershipService membershipService;
    // Obtener membresía activa del usuario
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMembershipByUserId(@PathVariable("userId") Long userId) {
        try {
            MembershipInfoDTO membership = membershipService.getActiveMembership(userId);
            if (membership == null) {
                return ResponseEntity.ok(Map.of("message", "El usuario no tiene membresía activa"));
            }
            return ResponseEntity.ok(membership);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Listar todos los planes
    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlanDTO>> getAllPlans() {
        return ResponseEntity.ok(membershipService.getAllPlans());
    }

    // Obtener plan por ID
    @GetMapping("/plans/{id}")
    public ResponseEntity<?> getPlan(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(membershipService.getPlanById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Crear nuevo plan (solo OWNER)
    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(
            @RequestBody MembershipPlanDTO planDto,
            Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            MembershipPlanDTO created = membershipService.createPlan(planDto, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // Actualizar plan (solo OWNER)
    @PutMapping("/plans/{id}")
    public ResponseEntity<?> updatePlan(
            @PathVariable("id") Long id,
            @RequestBody MembershipPlanDTO planDto,
            Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            MembershipPlanDTO updated = membershipService.updatePlan(id, planDto, currentUser);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // Eliminar plan (solo OWNER)
    @DeleteMapping("/plans/{id}")
    public ResponseEntity<?> deletePlan(
            @PathVariable("id") Long id,
            Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            membershipService.deletePlan(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Plan eliminado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // Asignar membresía a usuario (solo OWNER)
    @PostMapping("/assign")
    public ResponseEntity<?> assignPlanToUser(
            @RequestParam("userId") Long userId,
            @RequestParam("planId") Long planId,
            Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            membershipService.assignPlanToUser(userId, planId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Membresía asignada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
