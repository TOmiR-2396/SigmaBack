package com.example.gym.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExerciseDTO {
    public Long id;
    public String name;
    public String description;
    public String memberComment;
    public String trainerComment;
    public String videoUrl;
    public Long trainingPlanId;
    public Integer sets;
    public Integer reps;
    public Double weight; // en kg
    
    // Campos de progresión
    public Double previousWeight; // peso anterior para comparar
    public Double progressPercentage; // % de mejora en peso
    public LocalDateTime lastUpdatedAt; // última actualización
    public List<ProgressHistoryDTO> progressHistory; // histórico de cambios
    
    // Getters y setters
    public Double getPreviousWeight() { return previousWeight; }
    public void setPreviousWeight(Double previousWeight) { this.previousWeight = previousWeight; }
    
    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
    
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    
    public List<ProgressHistoryDTO> getProgressHistory() { return progressHistory; }
    public void setProgressHistory(List<ProgressHistoryDTO> progressHistory) { this.progressHistory = progressHistory; }
}
