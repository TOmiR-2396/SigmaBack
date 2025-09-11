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
}
