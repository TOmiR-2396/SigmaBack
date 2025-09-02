package com.example.gym.dto;

import java.util.List;

public class TrainingPlanDTO {
    public Long id;
    public String name;
    public String description;
    public Long userId;
    public List<ExerciseDTO> exercises;
}
