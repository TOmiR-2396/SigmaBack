package com.example.gym.controller;

import com.example.gym.model.Schedule;
import com.example.gym.service.ScheduleService;
import com.example.gym.tenant.FeatureFlagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de horarios con feature flags.
 * Ejemplo: pausar/reanudar días requiere PAUSE_DAYS habilitado.
 */
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * GET /api/schedules
     * Lista todos los horarios del tenant actual.
     */
    @GetMapping
    public ResponseEntity<?> getAllSchedules() {
        List<Schedule> schedules = scheduleService.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    /**
     * GET /api/schedules/{id}
     * Obtiene un horario específico.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSchedule(@PathVariable Long id) {
        try {
            Schedule schedule = scheduleService.getScheduleById(id);
            return ResponseEntity.ok(schedule);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/schedules
     * Crea un nuevo horario.
     */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<?> createSchedule(@RequestBody Schedule schedule) {
        try {
            Schedule created = scheduleService.createSchedule(schedule);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating schedule: " + e.getMessage());
        }
    }

    /**
     * PUT /api/schedules/{id}
     * Actualiza un horario.
     */
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        try {
            Schedule updated = scheduleService.updateSchedule(id, schedule);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/schedules/{id}
     * Elimina un horario.
     */
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        try {
            scheduleService.deleteSchedule(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/schedules/{id}/pause-day
     * Pausa un día específico en un horario.
     * Requiere feature PAUSE_DAYS habilitada.
     */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/pause-day")
    public ResponseEntity<?> pauseDay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String dateStr = body.get("date"); // YYYY-MM-DD
            LocalDate date = LocalDate.parse(dateStr);
            
            scheduleService.pauseDay(id, date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Día pausado exitosamente");
            response.put("scheduleId", id);
            response.put("date", date);
            return ResponseEntity.ok(response);
        } catch (FeatureFlagService.FeatureNotEnabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error pausing day: " + e.getMessage());
        }
    }

    /**
     * POST /api/schedules/{id}/resume-day
     * Reanuda un día pausado.
     */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/resume-day")
    public ResponseEntity<?> resumeDay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String dateStr = body.get("date"); // YYYY-MM-DD
            LocalDate date = LocalDate.parse(dateStr);
            
            scheduleService.resumeDay(id, date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Día reanudado exitosamente");
            response.put("scheduleId", id);
            response.put("date", date);
            return ResponseEntity.ok(response);
        } catch (FeatureFlagService.FeatureNotEnabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error resuming day: " + e.getMessage());
        }
    }

    /**
     * GET /api/schedules/{id}/is-paused
     * Verifica si un día está pausado.
     */
    @GetMapping("/{id}/is-paused")
    public ResponseEntity<?> isDayPaused(@PathVariable Long id, @RequestParam String date) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            boolean isPaused = scheduleService.isDayPaused(id, parsedDate);
            return ResponseEntity.ok(Map.of("isPaused", isPaused));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format");
        }
    }
}
