package com.example.gym.dto;

import java.time.LocalDateTime;

public class ProgressHistoryDTO {
    public Double weight;
    public Integer sets;
    public Integer reps;
    public LocalDateTime recordedAt;
    
    public ProgressHistoryDTO(Double weight, Integer sets, Integer reps, LocalDateTime recordedAt) {
        this.weight = weight;
        this.sets = sets;
        this.reps = reps;
        this.recordedAt = recordedAt;
    }
    
    // Getters
    public Double getWeight() { return weight; }
    public Integer getSets() { return sets; }
    public Integer getReps() { return reps; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
