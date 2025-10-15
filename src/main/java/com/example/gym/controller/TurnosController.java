package com.example.gym.controller;

import com.example.gym.dto.*;
import com.example.gym.model.*;
import com.example.gym.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/turnos")
public class TurnosController {
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;

    // =============== OWNER ENDPOINTS - Gestión de horarios ===============
    
    // POST /api/turnos/schedule - Crear horario
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleDTO scheduleDto) {
        try {
            // Validaciones
            if (scheduleDto.getDayOfWeek() == null || scheduleDto.getDayOfWeek() < 0 || scheduleDto.getDayOfWeek() > 6) {
                return ResponseEntity.badRequest().body("El día de la semana debe ser entre 0 (Domingo) y 6 (Sábado)");
            }
            if (scheduleDto.getStartTime() == null || scheduleDto.getEndTime() == null) {
                return ResponseEntity.badRequest().body("Las horas de inicio y fin son requeridas");
            }
            if (scheduleDto.getMaxCapacity() == null || scheduleDto.getMaxCapacity() <= 0) {
                return ResponseEntity.badRequest().body("La capacidad debe ser mayor a 0");
            }

            LocalTime startTime = LocalTime.parse(scheduleDto.getStartTime());
            LocalTime endTime = LocalTime.parse(scheduleDto.getEndTime());
            
            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest().body("La hora de inicio debe ser anterior a la hora de fin");
            }

            // Verificar duplicados exactos
            Long exactDuplicates = scheduleRepository.countExactDuplicate(
                scheduleDto.getDayOfWeek(), startTime, endTime);
            if (exactDuplicates > 0) {
                return ResponseEntity.badRequest().body("Ya existe un horario exacto para este día y horas");
            }

            // Verificar solapamientos de horarios
            List<Schedule> overlapping = scheduleRepository.findOverlappingSchedules(
                scheduleDto.getDayOfWeek(), startTime, endTime);
            if (!overlapping.isEmpty()) {
                Schedule conflict = overlapping.get(0);
                return ResponseEntity.badRequest().body(
                    "Este horario se solapa con otro existente: " + 
                    conflict.getStartTime() + " - " + conflict.getEndTime());
            }

            Schedule schedule = Schedule.builder()
                .dayOfWeek(scheduleDto.getDayOfWeek())
                .startTime(startTime)
                .endTime(endTime)
                .maxCapacity(scheduleDto.getMaxCapacity())
                .isActive(scheduleDto.getIsActive() != null ? scheduleDto.getIsActive() : true)
                .repeatWeekly(scheduleDto.getRepeatWeekly() != null ? scheduleDto.getRepeatWeekly() : true)
                .description(scheduleDto.getDescription())
                .build();

            Schedule saved = scheduleRepository.save(schedule);
            return ResponseEntity.ok(mapScheduleToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al crear horario: " + e.getMessage());
        }
    }

    // GET /api/turnos/schedules - Obtener todos los horarios
    @GetMapping("/schedules")
    @PreAuthorize("hasRole('OWNER') or hasRole('TRAINER')")
    public ResponseEntity<?> getSchedules() {
        List<Schedule> schedules = scheduleRepository.findAllActiveOrderedByDayAndTime();
        return ResponseEntity.ok(mapSchedulesToDTO(schedules));
    }

    // PUT /api/turnos/schedule/{scheduleId} - Actualizar horario
    @PutMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateSchedule(@PathVariable Long scheduleId, @RequestBody ScheduleDTO scheduleDto) {
        try {
            Optional<Schedule> existingOpt = scheduleRepository.findById(scheduleId);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Schedule existing = existingOpt.get();
            
            if (scheduleDto.getDayOfWeek() != null) {
                if (scheduleDto.getDayOfWeek() < 0 || scheduleDto.getDayOfWeek() > 6) {
                    return ResponseEntity.badRequest().body("El día de la semana debe ser entre 0 y 6");
                }
                existing.setDayOfWeek(scheduleDto.getDayOfWeek());
            }
            if (scheduleDto.getStartTime() != null) {
                existing.setStartTime(LocalTime.parse(scheduleDto.getStartTime()));
            }
            if (scheduleDto.getEndTime() != null) {
                existing.setEndTime(LocalTime.parse(scheduleDto.getEndTime()));
            }
            if (scheduleDto.getMaxCapacity() != null) {
                existing.setMaxCapacity(scheduleDto.getMaxCapacity());
            }
            if (scheduleDto.getIsActive() != null) {
                existing.setIsActive(scheduleDto.getIsActive());
            }
            if (scheduleDto.getRepeatWeekly() != null) {
                existing.setRepeatWeekly(scheduleDto.getRepeatWeekly());
            }
            if (scheduleDto.getDescription() != null) {
                existing.setDescription(scheduleDto.getDescription());
            }

            // Validar horas
            if (existing.getStartTime().isAfter(existing.getEndTime())) {
                return ResponseEntity.badRequest().body("La hora de inicio debe ser anterior a la hora de fin");
            }

            // Verificar solapamientos con otros horarios (excluyendo el actual)
            List<Schedule> overlapping = scheduleRepository.findOverlappingSchedulesExcludingCurrent(
                existing.getDayOfWeek(), existing.getStartTime(), existing.getEndTime(), scheduleId);
            if (!overlapping.isEmpty()) {
                Schedule conflict = overlapping.get(0);
                return ResponseEntity.badRequest().body(
                    "Este horario se solapa con otro existente: " + 
                    conflict.getStartTime() + " - " + conflict.getEndTime() + 
                    " (ID: " + conflict.getId() + ")");
            }

            Schedule saved = scheduleRepository.save(existing);
            return ResponseEntity.ok(mapScheduleToDTO(saved));
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
            Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
            if (scheduleOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // En lugar de eliminar físicamente, marcamos como inactivo
            Schedule schedule = scheduleOpt.get();
            schedule.setIsActive(false);
            scheduleRepository.save(schedule);

            return ResponseEntity.ok("Horario eliminado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al eliminar horario: " + e.getMessage());
        }
    }

    // =============== MEMBER ENDPOINTS - Reservas ===============
    
    // GET /api/turnos/available?date=2025-01-15 - Obtener turnos disponibles
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableSlots(@RequestParam String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            Integer dayOfWeek = targetDate.getDayOfWeek().getValue() % 7; // Convertir a 0=Domingo, 1=Lunes
            
            List<Schedule> schedulesForDay = scheduleRepository.findByDayOfWeekAndIsActive(dayOfWeek);
            
            List<AvailableSlotDTO> availableSlots = schedulesForDay.stream()
                .map(schedule -> {
                    Long confirmedReservations = reservationRepository
                        .countConfirmedReservationsByScheduleAndDate(schedule.getId(), targetDate);
                    
                    AvailableSlotDTO slot = new AvailableSlotDTO();
                    slot.setScheduleId(schedule.getId());
                    slot.setStartTime(schedule.getStartTime().toString());
                    slot.setEndTime(schedule.getEndTime().toString());
                    slot.setMaxCapacity(schedule.getMaxCapacity());
                    slot.setAvailableSpots(schedule.getMaxCapacity() - confirmedReservations.intValue());
                    
                    return slot;
                })
                .filter(slot -> slot.getAvailableSpots() > 0) // Solo slots disponibles
                .collect(Collectors.toList());
            
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
            
            // Validaciones
            if (request.getScheduleId() == null || request.getDate() == null) {
                return ResponseEntity.badRequest().body("El horario y fecha son requeridos");
            }
            
            LocalDate reservationDate = LocalDate.parse(request.getDate());
            
            // Verificar que el horario existe
            Optional<Schedule> scheduleOpt = scheduleRepository.findById(request.getScheduleId());
            if (scheduleOpt.isEmpty() || !scheduleOpt.get().getIsActive()) {
                return ResponseEntity.badRequest().body("Horario no encontrado o no disponible");
            }
            
            Schedule schedule = scheduleOpt.get();
            
            // Verificar que la fecha corresponde al día de la semana del horario
            Integer dateDayOfWeek = reservationDate.getDayOfWeek().getValue() % 7;
            if (!dateDayOfWeek.equals(schedule.getDayOfWeek())) {
                return ResponseEntity.badRequest().body("La fecha no corresponde al día de la semana del horario");
            }
            
            // Verificar que no se reserve para fechas pasadas
            if (reservationDate.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body("No se pueden hacer reservas para fechas pasadas");
            }
            
            // Verificar que el usuario no tenga ya una reserva CONFIRMADA para ese horario y fecha (fix para reservas canceladas)
            Long existingReservation = reservationRepository
                .countConfirmedByUserAndScheduleAndDate(currentUser.getId(), request.getScheduleId(), reservationDate);
            if (existingReservation > 0) {
                return ResponseEntity.badRequest().body("Ya tienes una reserva confirmada para este horario y fecha");
            }
            
            // Verificar capacidad disponible
            Long currentReservations = reservationRepository
                .countConfirmedReservationsByScheduleAndDate(request.getScheduleId(), reservationDate);
            if (currentReservations >= schedule.getMaxCapacity()) {
                return ResponseEntity.badRequest().body("No hay cupos disponibles para este horario");
            }
            
            // Crear la reserva
            Reservation reservation = Reservation.builder()
                .user(currentUser)
                .schedule(schedule)
                .date(reservationDate)
                .status(Reservation.ReservationStatus.CONFIRMED)
                .build();
            
            Reservation saved = reservationRepository.save(reservation);
            return ResponseEntity.ok(mapReservationToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al hacer reserva: " + e.getMessage());
        }
    }

    // DELETE /api/turnos/reservation/{reservationId} - Cancelar reserva
    @DeleteMapping("/reservation/{reservationId}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long reservationId, Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            
            Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Reservation reservation = reservationOpt.get();
            
            // Verificar que la reserva pertenece al usuario actual (a menos que sea OWNER/TRAINER)
            boolean isOwnerOrTrainer = currentUser.getRole() == User.UserRole.OWNER || 
                                      currentUser.getRole() == User.UserRole.TRAINER;
            
            if (!isOwnerOrTrainer && !reservation.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                   .body("No puedes cancelar esta reserva");
            }
            
            // Cambiar estado a cancelada y marcar fecha de cancelación
            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            reservation.setCancelledAt(LocalDateTime.now());
            reservationRepository.save(reservation);
            
            return ResponseEntity.ok("Reserva cancelada exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al cancelar reserva: " + e.getMessage());
        }
    }

    // GET /api/turnos/my-reservations - Obtener mis reservas
    @GetMapping("/my-reservations")
    public ResponseEntity<?> getUserReservations(Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            List<Reservation> reservations = reservationRepository.findByUserIdAndConfirmedStatus(currentUser.getId());
            
            // Verificar si no hay reservas
            if (reservations.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            List<ReservationDTO> reservationDTOs = mapReservationsToDTO(reservations);
            return ResponseEntity.ok(reservationDTOs);
        } catch (Exception e) {
            // Log del error para debug
            System.err.println("Error in getUserReservations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener reservas: " + e.getMessage());
        }
    }

    // =============== GENERAL ENDPOINTS ===============
    
    // GET /api/turnos/reservations?date=2025-01-15 - Obtener todas las reservas
    @GetMapping("/reservations")
    @PreAuthorize("hasRole('OWNER') or hasRole('TRAINER')")
    public ResponseEntity<?> getAllReservations(@RequestParam(required = false) String date) {
        try {
            List<Reservation> reservations;
            
            if (date != null && !date.isEmpty()) {
                LocalDate targetDate = LocalDate.parse(date);
                reservations = reservationRepository.findByDateAndConfirmedStatus(targetDate);
            } else {
                reservations = reservationRepository.findAllConfirmedReservations();
            }
            
            return ResponseEntity.ok(mapReservationsToDTO(reservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener reservas: " + e.getMessage());
        }
    }

    // =============== MÉTODOS DE MAPEO ===============
    
    private ScheduleDTO mapScheduleToDTO(Schedule schedule) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(schedule.getId());
        dto.setDayOfWeek(schedule.getDayOfWeek());
        dto.setStartTime(schedule.getStartTime().toString());
        dto.setEndTime(schedule.getEndTime().toString());
        dto.setMaxCapacity(schedule.getMaxCapacity());
        dto.setIsActive(schedule.getIsActive());
        dto.setRepeatWeekly(schedule.getRepeatWeekly());
        dto.setDescription(schedule.getDescription());
        
        // Calcular reservas actuales (opcional, para mostrar ocupación)
        Long currentReservations = reservationRepository
            .countConfirmedReservationsByScheduleAndDate(schedule.getId(), LocalDate.now());
        dto.setCurrentReservations(currentReservations.intValue());
        
        return dto;
    }
    
    private List<ScheduleDTO> mapSchedulesToDTO(List<Schedule> schedules) {
        return schedules.stream()
                       .map(this::mapScheduleToDTO)
                       .collect(Collectors.toList());
    }
    
    private ReservationDTO mapReservationToDTO(Reservation reservation) {
        try {
            ReservationDTO dto = new ReservationDTO();
            dto.setId(reservation.getId());
            dto.setScheduleId(reservation.getSchedule().getId());
            dto.setUserId(reservation.getUser().getId());
            dto.setDate(reservation.getDate().toString());
            dto.setStartTime(reservation.getSchedule().getStartTime().toString());
            dto.setEndTime(reservation.getSchedule().getEndTime().toString());
            dto.setStatus(reservation.getStatus().name());
            dto.setCreatedAt(reservation.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (reservation.getCancelledAt() != null) {
                dto.setCancelledAt(reservation.getCancelledAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            // Información del usuario - manejo seguro de valores nulos
            User user = reservation.getUser();
            if (user != null) {
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                dto.setUserName(firstName + " " + lastName);
                dto.setUserEmail(user.getEmail() != null ? user.getEmail() : "");
                dto.setUserPhone(user.getPhone() != null ? user.getPhone() : "");
            } else {
                dto.setUserName("Usuario no disponible");
                dto.setUserEmail("");
                dto.setUserPhone("");
            }
            
            return dto;
        } catch (Exception e) {
            // Log del error específico en el mapeo
            System.err.println("Error mapping reservation to DTO for reservation ID: " + reservation.getId());
            e.printStackTrace();
            
            // Crear un DTO básico en caso de error
            ReservationDTO errorDto = new ReservationDTO();
            errorDto.setId(reservation.getId());
            errorDto.setDate(reservation.getDate().toString());
            errorDto.setStatus(reservation.getStatus().name());
            errorDto.setUserName("Error al cargar datos");
            errorDto.setUserEmail("");
            errorDto.setUserPhone("");
            return errorDto;
        }
    }
    
    private List<ReservationDTO> mapReservationsToDTO(List<Reservation> reservations) {
        return reservations.stream()
                          .map(this::mapReservationToDTO)
                          .collect(Collectors.toList());
    }
}
