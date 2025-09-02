package com.example.gym.repository;

import com.example.gym.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByTrainingPlanId(Long planId);
}
