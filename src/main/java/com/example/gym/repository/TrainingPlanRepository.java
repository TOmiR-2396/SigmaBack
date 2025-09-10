package com.example.gym.repository;

import com.example.gym.model.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {
    // Buscar templates (planes sin usuario asignado)
    @Query("SELECT tp FROM TrainingPlan tp WHERE tp.isTemplate = true")
    List<TrainingPlan> findByIsTemplate(Boolean isTemplate);
    
    // Buscar planes asignados a usuarios (no templates)
    @Query("SELECT tp FROM TrainingPlan tp WHERE tp.user IS NOT NULL AND tp.isTemplate = false")
    List<TrainingPlan> findByUserIsNotNull();
    
    // Buscar planes de un usuario específico
    @Query("SELECT tp FROM TrainingPlan tp WHERE tp.user.id = :userId AND tp.isTemplate = false")
    List<TrainingPlan> findByUserId(@Param("userId") Long userId);
}
