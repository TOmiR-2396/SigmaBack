package com.example.gym.repository;

import com.example.gym.model.ExerciseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseProgressRepository extends JpaRepository<ExerciseProgress, Long> {
    // Obtener todo el histórico de un ejercicio, ordenado por fecha descendente
    List<ExerciseProgress> findByExerciseIdOrderByRecordedAtDesc(Long exerciseId);

    // Obtener el último registro de progreso de un ejercicio
    @Query(value = "SELECT * FROM exercise_progress WHERE exercise_id = :exerciseId ORDER BY recorded_at DESC LIMIT 1", nativeQuery = true)
    ExerciseProgress findLatestByExerciseId(@Param("exerciseId") Long exerciseId);
}
