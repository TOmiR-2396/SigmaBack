package com.example.gym.service;

import com.example.gym.dto.*;
import com.example.gym.model.*;
import com.example.gym.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TurnosService {
    
    private static final Logger logger = LoggerFactory.getLogger(TurnosService.class);
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * Crear nuevo horario (OWNER only)
     */
    public ScheduleDTO createSchedule(ScheduleDTO scheduleDto) {
        // Validaciones
        if (scheduleDto.getDayOfWeek() == null || scheduleDto.getDayOfWeek() < 0 || scheduleDto.getDayOfWeek() > 6) {
            throw new IllegalArgumentException("El día de la semana debe ser entre 0 (Domingo) y 6 (Sábado)");
        }
        if (scheduleDto.getStartTime() == null || scheduleDto.getEndTime() == null) {
            throw new IllegalArgumentException("Las horas de inicio y fin son requeridas");
        }
        if (scheduleDto.getMaxCapacity() == null || scheduleDto.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a 0");
        }

        LocalTime startTime = LocalTime.parse(scheduleDto.getStartTime());
        LocalTime endTime = LocalTime.parse(scheduleDto.getEndTime());
        
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }

        // Verificar duplicados exactos
        Long exactDuplicates = scheduleRepository.countExactDuplicate(
            scheduleDto.getDayOfWeek(), startTime, endTime);
        if (exactDuplicates > 0) {
            throw new IllegalArgumentException("Ya existe un horario exacto para este día y horas");
        }

        // Verificar solapamientos
        List<Schedule> overlapping = scheduleRepository.findOverlappingSchedules(
            scheduleDto.getDayOfWeek(), startTime, endTime);
        if (!overlapping.isEmpty()) {
            Schedule conflict = overlapping.get(0);
            throw new IllegalArgumentException(
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
        return mapScheduleToDTO(saved);
    }

    /**
     * Obtener todos los horarios activos
     */
    public List<ScheduleDTO> getSchedules() {
        List<Schedule> schedules = scheduleRepository.findAllActiveOrderedByDayAndTime();
        return mapSchedulesToDTO(schedules);
    }

    /**
     * Actualizar horario
     */
    public ScheduleDTO updateSchedule(Long scheduleId, ScheduleDTO scheduleDto) {
        Optional<Schedule> existingOpt = scheduleRepository.findById(scheduleId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Horario no encontrado");
        }

        Schedule existing = existingOpt.get();
        
        if (scheduleDto.getDayOfWeek() != null) {
            if (scheduleDto.getDayOfWeek() < 0 || scheduleDto.getDayOfWeek() > 6) {
                throw new IllegalArgumentException("El día de la semana debe ser entre 0 y 6");
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
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }

        // Verificar solapamientos excluyendo el actual
        List<Schedule> overlapping = scheduleRepository.findOverlappingSchedulesExcludingCurrent(
            existing.getDayOfWeek(), existing.getStartTime(), existing.getEndTime(), scheduleId);
        if (!overlapping.isEmpty()) {
            Schedule conflict = overlapping.get(0);
            throw new IllegalArgumentException(
                "Este horario se solapa con otro existente: " + 
                conflict.getStartTime() + " - " + conflict.getEndTime() + 
                " (ID: " + conflict.getId() + ")");
        }

        Schedule saved = scheduleRepository.save(existing);
        return mapScheduleToDTO(saved);
    }

    /**
     * Eliminar horario
     */
    public void deleteSchedule(Long scheduleId) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Horario no encontrado");
        }

        Schedule schedule = scheduleOpt.get();

        // Obtener reservas confirmadas para logging
        List<Reservation> confirmed = reservationRepository.findByScheduleIdAndStatus(scheduleId, Reservation.ReservationStatus.CONFIRMED);
        if (!confirmed.isEmpty()) {
            logger.info("Eliminando schedule {} que tiene {} reservas confirmadas", scheduleId, confirmed.size());
        }

        scheduleRepository.delete(schedule);
    }

    /**
     * Obtener turnos disponibles para una fecha
     */
    public List<AvailableSlotDTO> getAvailableSlots(String date) {
        LocalDate targetDate = LocalDate.parse(date);
        Integer dayOfWeek = targetDate.getDayOfWeek().getValue() % 7; // 0=Domingo, 1=Lunes
        
        List<Schedule> schedulesForDay = scheduleRepository.findByDayOfWeekAndIsActive(dayOfWeek);
        
        return schedulesForDay.stream()
            .filter(schedule -> !isDatePaused(schedule, targetDate))
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
            .filter(slot -> slot.getAvailableSpots() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Hacer reserva con bloqueo pessimista
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ReservationDTO makeReservation(ReservationRequest request, User currentUser) {
        // Validaciones
        if (request.getScheduleId() == null || request.getDate() == null) {
            throw new IllegalArgumentException("El horario y fecha son requeridos");
        }
        
        LocalDate reservationDate = LocalDate.parse(request.getDate());
        
        // Verificar que el horario existe con bloqueo pesimista
        Optional<Schedule> scheduleOpt = scheduleRepository.findByIdForUpdate(request.getScheduleId());
        if (scheduleOpt.isEmpty() || !scheduleOpt.get().getIsActive()) {
            throw new IllegalArgumentException("Horario no encontrado o no disponible");
        }
        
        Schedule schedule = scheduleOpt.get();
        
        // Verificar que la fecha corresponde al día de la semana del horario
        Integer dateDayOfWeek = reservationDate.getDayOfWeek().getValue() % 7;
        if (!dateDayOfWeek.equals(schedule.getDayOfWeek())) {
            throw new IllegalArgumentException("La fecha no corresponde al día de la semana del horario");
        }
        
        // Verificar que no se reserve para fechas pasadas
        if (reservationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden hacer reservas para fechas pasadas");
        }

        // Bloqueo por pausa en fecha específica
        if (isDatePaused(schedule, reservationDate)) {
            throw new IllegalArgumentException("No se permiten reservas en esta fecha (pausada)");
        }
        
        // Verificar que el usuario no tenga una reserva confirmada para ese horario y fecha
        Long existingReservation = reservationRepository
            .countConfirmedByUserAndScheduleAndDate(currentUser.getId(), request.getScheduleId(), reservationDate);
        if (existingReservation != null && existingReservation > 0) {
            throw new IllegalArgumentException("Ya tienes una reserva confirmada para este horario y fecha");
        }
        
        // Verificar capacidad disponible
        Long currentReservations = reservationRepository
            .countConfirmedReservationsByScheduleAndDate(request.getScheduleId(), reservationDate);
        if (currentReservations >= schedule.getMaxCapacity()) {
            throw new IllegalArgumentException("No hay cupos disponibles para este horario");
        }
        
        // Crear la reserva
        Reservation reservation = Reservation.builder()
            .user(currentUser)
            .schedule(schedule)
            .date(reservationDate)
            .status(Reservation.ReservationStatus.CONFIRMED)
            .build();
        
        Reservation saved = reservationRepository.saveAndFlush(reservation);
        return mapReservationToDTO(saved);
    }

    /**
     * Pausar un día específico para un schedule
     */
    public String pauseScheduleDay(Long scheduleId, String date) {
        LocalDate target = LocalDate.parse(date);
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Horario no encontrado (ID: " + scheduleId + ")");
        }
        Schedule schedule = scheduleOpt.get();

        // Validar que la fecha corresponde al día del schedule
        Integer dow = target.getDayOfWeek().getValue() % 7;
        if (!dow.equals(schedule.getDayOfWeek())) {
            throw new IllegalArgumentException("La fecha no corresponde al día del horario");
        }

        // Añadir fecha a pausedDates
        schedule.setPausedDates(addDateToCsv(schedule.getPausedDates(), target));
        scheduleRepository.save(schedule);

        // Cancelar reservas confirmadas
        List<Reservation> toCancel = reservationRepository
                .findByScheduleIdAndDateAndStatus(scheduleId, target, Reservation.ReservationStatus.CONFIRMED);
        for (Reservation r : toCancel) {
            r.setStatus(Reservation.ReservationStatus.CANCELLED);
            r.setCancelledAt(LocalDateTime.now());
        }
        if (!toCancel.isEmpty()) {
            reservationRepository.saveAll(toCancel);
        }

        return "Día pausado y reservas del día canceladas";
    }

    /**
     * Quitar pausa de un día específico
     */
    public String unpauseScheduleDay(Long scheduleId, String date) {
        LocalDate target = LocalDate.parse(date);
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Horario no encontrado (ID: " + scheduleId + ")");
        }
        Schedule schedule = scheduleOpt.get();

        schedule.setPausedDates(removeDateFromCsv(schedule.getPausedDates(), target));
        scheduleRepository.save(schedule);
        return "Pausa retirada para la fecha";
    }

    /**
     * Pausar día completo (todos los schedules activos)
     */
    public String pauseEntireDay(String date) {
        LocalDate target = LocalDate.parse(date);
        Integer dayOfWeek = target.getDayOfWeek().getValue() % 7;
        
        List<Schedule> schedulesForDay = scheduleRepository.findByDayOfWeekAndIsActive(dayOfWeek);
        if (schedulesForDay.isEmpty()) {
            return "No hay horarios activos para pausar en este día";
        }

        int pausedCount = 0;
        int cancelledReservations = 0;
        
        for (Schedule schedule : schedulesForDay) {
            schedule.setPausedDates(addDateToCsv(schedule.getPausedDates(), target));
            scheduleRepository.save(schedule);
            pausedCount++;
            
            List<Reservation> toCancel = reservationRepository
                    .findByScheduleIdAndDateAndStatus(schedule.getId(), target, Reservation.ReservationStatus.CONFIRMED);
            for (Reservation r : toCancel) {
                r.setStatus(Reservation.ReservationStatus.CANCELLED);
                r.setCancelledAt(LocalDateTime.now());
                cancelledReservations++;
            }
            if (!toCancel.isEmpty()) {
                reservationRepository.saveAll(toCancel);
            }
        }
        
        return String.format(
            "Día pausado: %d horarios pausados, %d reservas canceladas",
            pausedCount, cancelledReservations);
    }

    /**
     * Despausar día completo
     */
    public String unpauseEntireDay(String date) {
        LocalDate target = LocalDate.parse(date);
        Integer dayOfWeek = target.getDayOfWeek().getValue() % 7;
        
        List<Schedule> schedulesForDay = scheduleRepository.findByDayOfWeekAndIsActive(dayOfWeek);
        if (schedulesForDay.isEmpty()) {
            return "No hay horarios activos para despausar en este día";
        }

        int unpausedCount = 0;
        for (Schedule schedule : schedulesForDay) {
            schedule.setPausedDates(removeDateFromCsv(schedule.getPausedDates(), target));
            scheduleRepository.save(schedule);
            unpausedCount++;
        }
        
        return String.format("Día despausado: %d horarios despausados", unpausedCount);
    }

    /**
     * Cancelar una reserva
     */
    public String cancelReservation(Long reservationId, User currentUser) {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        
        Reservation reservation = reservationOpt.get();
        
        // Verificar permisos
        boolean isOwnerOrTrainer = currentUser.getRole() == User.UserRole.OWNER || 
                                  currentUser.getRole() == User.UserRole.TRAINER;
        
        if (!isOwnerOrTrainer && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("No puedes cancelar esta reserva");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        
        return "Reserva cancelada exitosamente";
    }

    /**
     * Marcar asistencia
     */
    public ReservationDTO markAttendance(Long reservationId, AttendanceRequest request) {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        
        Reservation reservation = reservationOpt.get();
        
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Solo se puede marcar asistencia en reservas confirmadas");
        }
        
        reservation.setAttended(request.getAttended());
        
        if (request.getAttended()) {
            reservation.setAttendedAt(LocalDateTime.now());
        } else {
            reservation.setAttendedAt(null);
        }
        
        reservationRepository.save(reservation);
        return mapReservationToDTO(reservation);
    }

    /**
     * Obtener mis reservas
     */
    public List<ReservationDTO> getUserReservations(User currentUser) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndConfirmedStatus(currentUser.getId());
        
        if (reservations.isEmpty()) {
            return new ArrayList<>();
        }
        
        return mapReservationsToDTO(reservations);
    }

    /**
     * Obtener todas las reservas (con filtro opcional por fecha)
     */
    public List<ReservationDTO> getAllReservations(String date) {
        List<Reservation> reservations;
        
        if (date != null && !date.isEmpty()) {
            LocalDate targetDate = LocalDate.parse(date);
            reservations = reservationRepository.findByDateAndConfirmedStatus(targetDate);
        } else {
            reservations = reservationRepository.findAllConfirmedReservations();
        }
        
        return mapReservationsToDTO(reservations);
    }

    /**
     * Verificar si una fecha está pausada
     */
    private boolean isDatePaused(Schedule schedule, LocalDate date) {
        if (schedule.getPausedDates() == null || schedule.getPausedDates().isEmpty()) return false;
        String iso = date.toString();
        for (String token : schedule.getPausedDates().split(",")) {
            String t = token.trim();
            if (!t.isEmpty() && t.equals(iso)) return true;
        }
        return false;
    }

    /**
     * Agregar fecha a CSV de fechas pausadas
     */
    private String addDateToCsv(String csv, LocalDate date) {
        String iso = date.toString();
        if (csv == null || csv.isEmpty()) return iso;
        for (String token : csv.split(",")) {
            if (iso.equals(token.trim())) return csv;
        }
        return csv + "," + iso;
    }

    /**
     * Remover fecha de CSV de fechas pausadas
     */
    private String removeDateFromCsv(String csv, LocalDate date) {
        if (csv == null || csv.isEmpty()) return csv;
        String iso = date.toString();
        String[] parts = csv.split(",");
        List<String> kept = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty() && !t.equals(iso)) kept.add(t);
        }
        return String.join(",", kept);
    }

    /**
     * Mapear Schedule a DTO
     */
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
        
        Long currentReservations = reservationRepository
            .countConfirmedReservationsByScheduleAndDate(schedule.getId(), LocalDate.now());
        dto.setCurrentReservations(currentReservations.intValue());
        
        return dto;
    }
    
    /**
     * Mapear lista de Schedules a DTOs
     */
    private List<ScheduleDTO> mapSchedulesToDTO(List<Schedule> schedules) {
        return schedules.stream()
                       .map(this::mapScheduleToDTO)
                       .collect(Collectors.toList());
    }
    
    /**
     * Mapear Reservation a DTO
     */
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
            
            dto.setAttended(reservation.getAttended() != null ? reservation.getAttended() : false);
            if (reservation.getAttendedAt() != null) {
                dto.setAttendedAt(reservation.getAttendedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
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
            logger.error("Error mapeando reserva ID: " + reservation.getId(), e);
            
            ReservationDTO errorDto = new ReservationDTO();
            errorDto.setId(reservation.getId());
            errorDto.setDate(reservation.getDate().toString());
            errorDto.setStatus(reservation.getStatus().name());
            errorDto.setAttended(false);
            errorDto.setUserName("Error al cargar datos");
            errorDto.setUserEmail("");
            errorDto.setUserPhone("");
            return errorDto;
        }
    }
    
    /**
     * Mapear lista de Reservations a DTOs
     */
    private List<ReservationDTO> mapReservationsToDTO(List<Reservation> reservations) {
        return reservations.stream()
                          .map(this::mapReservationToDTO)
                          .collect(Collectors.toList());
    }
}
