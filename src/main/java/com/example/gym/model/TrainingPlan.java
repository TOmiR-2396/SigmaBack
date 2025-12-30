package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import java.util.List;

@Entity
@Table(name = "planes")
@Filter(name = "tenantFilter")
public class TrainingPlan extends TenantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Boolean isTemplate = false;

    // Relación con el usuario miembro (nullable si es template)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;


    // Relación con ejercicios
    @OneToMany(mappedBy = "trainingPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Exercise> exercises;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Boolean getIsTemplate() { return isTemplate; }
    public void setIsTemplate(Boolean isTemplate) { this.isTemplate = isTemplate; }
    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }
}
