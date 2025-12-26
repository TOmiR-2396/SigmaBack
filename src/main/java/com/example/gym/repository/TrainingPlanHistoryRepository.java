package com.example.gym.repository;

import com.example.gym.model.TrainingPlanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingPlanHistoryRepository extends JpaRepository<TrainingPlanHistory, Long> {
    // Obtener histórico de un plan, ordenado por fecha descendente
    List<TrainingPlanHistory> findByTrainingPlanIdOrderByArchivedAtDesc(Long trainingPlanId);

    // Obtener histórico de planes de un usuario
    List<TrainingPlanHistory> findByUserIdOrderByArchivedAtDesc(Long userId);

    // Obtener planes archivados en un rango de fechas
    @Query(value = "SELECT * FROM training_plan_history WHERE training_plan_id = :planId AND archived_at BETWEEN :startDate AND :endDate ORDER BY archived_at DESC", nativeQuery = true)
    List<TrainingPlanHistory> findHistoryByDateRange(@Param("planId") Long planId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
