package com.example.gym.controller;

import com.example.gym.dto.AnalyticsDTO;
import com.example.gym.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * AnalyticsController: Endpoints para métricas y analítica de SigmaGym
 * Solo OWNER puede acceder
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Analytics general completo (solo OWNER)
     */
    @GetMapping
    public ResponseEntity<?> getGeneralAnalytics(Authentication auth) {
        try {
            com.example.gym.model.User currentUser = (com.example.gym.model.User) auth.getPrincipal();
            if (currentUser.getRole() != com.example.gym.model.User.UserRole.OWNER) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "Solo OWNER puede acceder a analytics"));
            }
            return ResponseEntity.ok(analyticsService.getGeneralAnalytics());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "No autorizado"));
        }
    }

    /**
     * Analytics de membresías (solo OWNER)
     */
    @GetMapping("/memberships")
    public ResponseEntity<?> getMembershipAnalytics(Authentication auth) {
        try {
            com.example.gym.model.User currentUser = (com.example.gym.model.User) auth.getPrincipal();
            if (currentUser.getRole() != com.example.gym.model.User.UserRole.OWNER) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "Solo OWNER puede acceder a analytics"));
            }
            return ResponseEntity.ok(analyticsService.getMembershipAnalytics());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "No autorizado"));
        }
    }

    /**
     * Analytics de asistencia (solo OWNER)
     */
    @GetMapping("/attendance")
    public ResponseEntity<?> getAttendanceAnalytics(Authentication auth) {
        try {
            com.example.gym.model.User currentUser = (com.example.gym.model.User) auth.getPrincipal();
            if (currentUser.getRole() != com.example.gym.model.User.UserRole.OWNER) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "Solo OWNER puede acceder a analytics"));
            }
            return ResponseEntity.ok(analyticsService.getAttendanceAnalytics());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "No autorizado"));
        }
    }

    /**
     * Analytics de ejercicios (solo OWNER)
     */
    @GetMapping("/exercises")
    public ResponseEntity<?> getExerciseAnalytics(Authentication auth) {
        try {
            com.example.gym.model.User currentUser = (com.example.gym.model.User) auth.getPrincipal();
            if (currentUser.getRole() != com.example.gym.model.User.UserRole.OWNER) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "Solo OWNER puede acceder a analytics"));
            }
            return ResponseEntity.ok(analyticsService.getExerciseAnalytics());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "No autorizado"));
        }
    }

    /**
     * Analytics de trainers (solo OWNER)
     */
    @GetMapping("/trainers")
    public ResponseEntity<?> getTrainerAnalytics(Authentication auth) {
        try {
            com.example.gym.model.User currentUser = (com.example.gym.model.User) auth.getPrincipal();
            if (currentUser.getRole() != com.example.gym.model.User.UserRole.OWNER) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "Solo OWNER puede acceder a analytics"));
            }
            return ResponseEntity.ok(analyticsService.getTrainerAnalytics());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "No autorizado"));
        }
    }
}
