package com.example.gym.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_plan_history")
public class TrainingPlanHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String exercisesSnapshot; // JSON snapshot de ejercicios en ese momento

    @Column(nullable = false, updatable = false)
    private LocalDateTime archivedAt;

    @Column(length = 500)
    private String notes; // Notas del cambio de plan

    // Constructores
    public TrainingPlanHistory() {
        this.archivedAt = LocalDateTime.now();
    }

    public TrainingPlanHistory(TrainingPlan plan, User user, String exercisesSnapshot) {
        this.trainingPlan = plan;
        this.user = user;
        this.startDate = plan.getStartDate();
        this.endDate = plan.getEndDate();
        this.exercisesSnapshot = exercisesSnapshot;
        this.archivedAt = LocalDateTime.now();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TrainingPlan getTrainingPlan() { return trainingPlan; }
    public void setTrainingPlan(TrainingPlan trainingPlan) { this.trainingPlan = trainingPlan; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getExercisesSnapshot() { return exercisesSnapshot; }
    public void setExercisesSnapshot(String exercisesSnapshot) { this.exercisesSnapshot = exercisesSnapshot; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
