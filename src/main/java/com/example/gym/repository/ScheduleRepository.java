package com.example.gym.repository;

import com.example.gym.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    // Buscar horarios por día de la semana y que estén activos
    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.isActive = true")
    List<Schedule> findByDayOfWeekAndIsActive(@Param("dayOfWeek") Integer dayOfWeek);
    
    // Obtener todos los horarios activos ordenados por día y hora
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true ORDER BY s.dayOfWeek, s.startTime")
    List<Schedule> findAllActiveOrderedByDayAndTime();
    
    // Buscar horarios activos
    List<Schedule> findByIsActiveTrue();
    
    // Verificar si existe un horario exacto duplicado (mismo día, hora inicio y fin)
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek " +
           "AND s.startTime = :startTime AND s.endTime = :endTime AND s.isActive = true")
    Long countExactDuplicate(@Param("dayOfWeek") Integer dayOfWeek, 
                            @Param("startTime") java.time.LocalTime startTime, 
                            @Param("endTime") java.time.LocalTime endTime);
    
    // Verificar horarios que se solapan en el mismo día
    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.isActive = true " +
           "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    List<Schedule> findOverlappingSchedules(@Param("dayOfWeek") Integer dayOfWeek,
                                          @Param("startTime") java.time.LocalTime startTime,
                                          @Param("endTime") java.time.LocalTime endTime);
    
    // Para actualización - excluir el horario actual de la verificación
    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.isActive = true " +
           "AND s.id != :excludeId " +
           "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    List<Schedule> findOverlappingSchedulesExcludingCurrent(@Param("dayOfWeek") Integer dayOfWeek,
                                                           @Param("startTime") java.time.LocalTime startTime,
                                                           @Param("endTime") java.time.LocalTime endTime,
                                                           @Param("excludeId") Long excludeId);
}
