package com.example.gym.controller;

import com.example.gym.dto.*;
import com.example.gym.model.*;
import com.example.gym.service.TurnosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnosController {
    
    private static final Logger logger = LoggerFactory.getLogger(TurnosController.class);
    
    @Autowired
    private TurnosService turnosService;

    
    // POST /api/turnos/schedule - Crear horario
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleDTO scheduleDto) {
        try {
            ScheduleDTO result = turnosService.createSchedule(scheduleDto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al crear horario: " + e.getMessage());
        }
    }

    // GET /api/turnos/schedules - Obtener todos los horarios
    @GetMapping("/schedules")
    @PreAuthorize("hasRole('OWNER') or hasRole('TRAINER')")
    public ResponseEntity<?> getSchedules() {
        try {
            List<ScheduleDTO> schedules = turnosService.getSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener horarios: " + e.getMessage());
        }
    }

    // PUT /api/turnos/schedule/{scheduleId} - Actualizar horario
    @PutMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateSchedule(@PathVariable Long scheduleId, @RequestBody ScheduleDTO scheduleDto) {
        try {
            ScheduleDTO result = turnosService.updateSchedule(scheduleId, scheduleDto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al actualizar horario: " + e.getMessage());
        }
    }

    // DELETE /api/turnos/schedule/{scheduleId} - Eliminar horario
    @DeleteMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long scheduleId) {
        try {
            turnosService.deleteSchedule(scheduleId);
            return ResponseEntity.ok("Schedule y sus reservas eliminados exitosamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error al eliminar schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al eliminar schedule");
        }
    }

    // GET /api/turnos/available?date=2025-01-15 - Obtener turnos disponibles
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableSlots(@RequestParam String date) {
        try {
            List<AvailableSlotDTO> availableSlots = turnosService.getAvailableSlots(date);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener turnos disponibles: " + e.getMessage());
        }
    }

    // POST /api/turnos/reservation - Hacer reserva
    @PostMapping("/reservation")
    public ResponseEntity<?> makeReservation(@RequestBody ReservationRequest request, Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            ReservationDTO result = turnosService.makeReservation(request, currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No se pueden hacer reservas")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            } else if (e.getMessage().contains("Ya tienes una reserva")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("Reserva duplicada o cupo agotado por concurrencia. Intenta nuevamente.");
        } catch (jakarta.persistence.PessimisticLockException | ObjectOptimisticLockingFailureException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("El turno está siendo reservado por otros usuarios. Intenta nuevamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al hacer reserva: " + e.getMessage());
        }
    }

    // OWNER: Pausar un día específico para un schedule
    @PutMapping("/schedule/{scheduleId}/pause")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> pauseScheduleDay(@PathVariable Long scheduleId, @RequestParam String date) {
        try {
            String result = turnosService.pauseScheduleDay(scheduleId, date);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al pausar día: " + e.getMessage());
        }
    }

    // OWNER: Quitar pausa de un día específico
    @DeleteMapping("/schedule/{scheduleId}/pause")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> unpauseScheduleDay(@PathVariable Long scheduleId, @RequestParam String date) {
        try {
            String result = turnosService.unpauseScheduleDay(scheduleId, date);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al quitar pausa: " + e.getMessage());
        }
    }

    // OWNER: Pausar DÍA COMPLETO (todos los schedules activos de ese día)
    @PutMapping("/pause-day")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> pauseEntireDay(@RequestParam String date) {
        try {
            String result = turnosService.pauseEntireDay(date);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error al pausar día completo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al pausar día completo: " + e.getMessage());
        }
    }

    // OWNER: Despausar DÍA COMPLETO (todos los schedules del día)
    @DeleteMapping("/pause-day")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> unpauseEntireDay(@RequestParam String date) {
        try {
            String result = turnosService.unpauseEntireDay(date);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error al despausar día completo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al despausar día completo: " + e.getMessage());
        }
    }

    // DELETE /api/turnos/reservation/{reservationId} - Cancelar reserva
    @DeleteMapping("/reservation/{reservationId}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long reservationId, Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            String result = turnosService.cancelReservation(reservationId, currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No puedes cancelar")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al cancelar reserva: " + e.getMessage());
        }
    }

    // PUT /api/turnos/reservation/{reservationId}/attendance - Marcar presentismo/asistencia
    @PutMapping("/reservation/{reservationId}/attendance")
    @PreAuthorize("hasRole('OWNER') or hasRole('TRAINER')")
    public ResponseEntity<?> markAttendance(@PathVariable Long reservationId, @RequestBody AttendanceRequest request) {
        try {
            ReservationDTO result = turnosService.markAttendance(reservationId, request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al marcar asistencia: " + e.getMessage());
        }
    }

    // GET /api/turnos/my-reservations - Obtener mis reservas
    @GetMapping("/my-reservations")
    public ResponseEntity<?> getUserReservations(Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            List<ReservationDTO> reservationDTOs = turnosService.getUserReservations(currentUser);
            return ResponseEntity.ok(reservationDTOs);
        } catch (Exception e) {
            logger.error("Error al obtener reservas del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener reservas");
        }
    }

    // GET /api/turnos/reservations?date=2025-01-15 - Obtener todas las reservas
    @GetMapping("/reservations")
    @PreAuthorize("hasRole('OWNER') or hasRole('TRAINER')")
    public ResponseEntity<?> getAllReservations(@RequestParam(required = false) String date) {
        try {
            List<ReservationDTO> reservationDTOs = turnosService.getAllReservations(date);
            return ResponseEntity.ok(reservationDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener reservas: " + e.getMessage());
        }
    }
}