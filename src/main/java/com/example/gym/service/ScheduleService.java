package com.example.gym.service;

import com.example.gym.model.Schedule;
import com.example.gym.repository.ScheduleRepository;
import com.example.gym.tenant.FeatureFlagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de Schedule que integra feature flags.
 * Ejemplo: pausar días requiere que la feature PAUSE_DAYS esté habilitada.
 */
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final FeatureFlagService featureFlagService;

    public ScheduleService(ScheduleRepository scheduleRepository, FeatureFlagService featureFlagService) {
        this.scheduleRepository = scheduleRepository;
        this.featureFlagService = featureFlagService;
    }

    /**
     * Obtiene un horario por ID.
     */
    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    /**
     * Lista todos los horarios del tenant actual.
     */
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    /**
     * Pausa un día específico en un horario.
     * Requiere que PAUSE_DAYS esté habilitado.
     */
    @Transactional
    public void pauseDay(Long scheduleId, LocalDate date) {
        // Guard-clause: verifica si la feature está habilitada
        featureFlagService.requireEnabled(
            "PAUSE_DAYS",
            "La pausa de horarios no está habilitada en este gimnasio"
        );

        Schedule schedule = getScheduleById(scheduleId);

        // Obtener fechas pausadas actuales
        String pausedDates = schedule.getPausedDates();
        String dateStr = date.toString(); // YYYY-MM-DD

        if (pausedDates == null || pausedDates.isBlank()) {
            schedule.setPausedDates(dateStr);
        } else if (!pausedDates.contains(dateStr)) {
            schedule.setPausedDates(pausedDates + "," + dateStr);
        }

        scheduleRepository.save(schedule);
    }

    /**
     * Reanuda un día pausado.
     * También requiere PAUSE_DAYS.
     */
    @Transactional
    public void resumeDay(Long scheduleId, LocalDate date) {
        featureFlagService.requireEnabled(
            "PAUSE_DAYS",
            "La pausa de horarios no está habilitada en este gimnasio"
        );

        Schedule schedule = getScheduleById(scheduleId);
        String pausedDates = schedule.getPausedDates();

        if (pausedDates != null && !pausedDates.isBlank()) {
            String dateStr = date.toString();
            String updated = pausedDates.replace(dateStr, "")
                .replaceAll(",,+", ",")
                .replaceAll("^,|,$", "");
            schedule.setPausedDates(updated.isBlank() ? null : updated);
            scheduleRepository.save(schedule);
        }
    }

    /**
     * Crea un nuevo horario.
     */
    @Transactional
    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    /**
     * Actualiza un horario existente.
     */
    @Transactional
    public Schedule updateSchedule(Long id, Schedule updatedSchedule) {
        Schedule schedule = getScheduleById(id);
        schedule.setDayOfWeek(updatedSchedule.getDayOfWeek());
        schedule.setStartTime(updatedSchedule.getStartTime());
        schedule.setEndTime(updatedSchedule.getEndTime());
        schedule.setMaxCapacity(updatedSchedule.getMaxCapacity());
        schedule.setIsActive(updatedSchedule.getIsActive());
        schedule.setRepeatWeekly(updatedSchedule.getRepeatWeekly());
        schedule.setDescription(updatedSchedule.getDescription());
        return scheduleRepository.save(schedule);
    }

    /**
     * Elimina un horario.
     */
    @Transactional
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    /**
     * Verifica si un día está pausado en un horario.
     */
    public boolean isDayPaused(Long scheduleId, LocalDate date) {
        Schedule schedule = getScheduleById(scheduleId);
        String pausedDates = schedule.getPausedDates();
        if (pausedDates == null || pausedDates.isBlank()) {
            return false;
        }
        return pausedDates.contains(date.toString());
    }
}
