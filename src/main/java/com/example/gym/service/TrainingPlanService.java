package com.example.gym.service;

import com.example.gym.dto.ExerciseDTO;
import com.example.gym.dto.TrainingPlanDTO;
import com.example.gym.model.Exercise;
import com.example.gym.model.TrainingPlan;
import com.example.gym.model.TrainingPlanHistory;
import com.example.gym.model.User;
import com.example.gym.repository.ExerciseRepository;
import com.example.gym.repository.TrainingPlanRepository;
import com.example.gym.repository.TrainingPlanHistoryRepository;
import com.example.gym.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TrainingPlanService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingPlanService.class);
    
    @Autowired
    private TrainingPlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private TrainingPlanHistoryRepository historyRepository;
    @Autowired
    private ObjectMapper objectMapper;
    
    // ==================== PLANTILLAS ====================
    
    /**
     * Crear una nueva plantilla
     */
    public TrainingPlan createTemplate(TrainingPlanDTO planDto) {
        if (planDto.name == null || planDto.name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la plantilla es requerido");
        }
        
        TrainingPlan template = new TrainingPlan();
        template.setName(planDto.name.trim());
        template.setDescription(planDto.description != null ? planDto.description.trim() : "");
        template.setIsTemplate(true);
        template.setUser(null); // Templates no tienen usuario asignado
        
        return planRepository.save(template);
    }
    
    /**
     * Actualizar plantilla existente (incluyendo sus ejercicios)
     */
    public TrainingPlan updateTemplate(Long templateId, TrainingPlanDTO planDto) {
        Optional<TrainingPlan> templateOpt = planRepository.findById(templateId);
        if (templateOpt.isEmpty() || !Boolean.TRUE.equals(templateOpt.get().getIsTemplate())) {
            throw new IllegalArgumentException("Template no encontrado");
        }
        
        TrainingPlan template = templateOpt.get();
        
        // Actualizar campos básicos
        if (planDto.name != null && !planDto.name.trim().isEmpty()) {
            template.setName(planDto.name.trim());
        }
        if (planDto.description != null) {
            template.setDescription(planDto.description.trim());
        }
        
        template.setIsTemplate(true);
        template.setUser(null);
        
        // Actualizar ejercicios si vienen en el DTO
        if (planDto.exercises != null) {
            List<Exercise> existingExercises = exerciseRepository.findByTrainingPlanId(templateId);
            exerciseRepository.deleteAll(existingExercises);
            
            for (ExerciseDTO exerciseDto : planDto.exercises) {
                if (exerciseDto.name != null && !exerciseDto.name.trim().isEmpty()) {
                    Exercise exercise = new Exercise();
                    exercise.setTrainingPlan(template);
                    exercise.setName(exerciseDto.name.trim());
                    exercise.setDescription(exerciseDto.description != null ? exerciseDto.description : "");
                    exercise.setSets(exerciseDto.sets != null ? exerciseDto.sets : 0);
                    exercise.setReps(exerciseDto.reps != null ? exerciseDto.reps : 0);
                    exercise.setWeight(exerciseDto.weight != null ? exerciseDto.weight : 0.0);
                    exercise.setVideoUrl(exerciseDto.videoUrl);
                    
                    exerciseRepository.save(exercise);
                }
            }
        }
        
        return planRepository.save(template);
    }
    
    /**
     * Eliminar una plantilla
     */
    public void deleteTemplate(Long templateId) {
        Optional<TrainingPlan> template = planRepository.findById(templateId);
        if (template.isEmpty() || !Boolean.TRUE.equals(template.get().getIsTemplate())) {
            throw new IllegalArgumentException("Template not found");
        }
        planRepository.deleteById(templateId);
    }
    
    /**
     * Asignar una plantilla a un usuario (crea copia)
     */
    public TrainingPlan assignTemplateToUser(Long templateId, Long userId) {
        Optional<TrainingPlan> templateOpt = planRepository.findById(templateId);
        if (templateOpt.isEmpty() || !Boolean.TRUE.equals(templateOpt.get().getIsTemplate())) {
            throw new IllegalArgumentException("Template no encontrado");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        
        // Crear COPIA del template para el usuario
        TrainingPlan template = templateOpt.get();
        TrainingPlan userPlan = new TrainingPlan();
        userPlan.setName(template.getName() + " - " + userOpt.get().getFirstName());
        userPlan.setDescription(template.getDescription());
        userPlan.setUser(userOpt.get());
        userPlan.setIsTemplate(false);
        
        return planRepository.save(userPlan);
    }
    
    // ==================== PLANES DE USUARIO ====================
    
    /**
     * Crear plan directo para un usuario
     */
    public TrainingPlan createPlan(TrainingPlanDTO planDto) {
        if (planDto.name == null || planDto.name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del plan es requerido");
        }
        if (planDto.userId == null) {
            throw new IllegalArgumentException("El ID del usuario es requerido");
        }
        
        Optional<User> member = userRepository.findById(planDto.userId);
        if (member.isEmpty()) {
            throw new IllegalArgumentException("Target user not found");
        }
        
        TrainingPlan plan = new TrainingPlan();
        plan.setName(planDto.name.trim());
        plan.setDescription(planDto.description != null ? planDto.description.trim() : "");
        plan.setIsTemplate(false);
        plan.setUser(member.get());
        
        return planRepository.save(plan);
    }
    
    /**
     * Actualizar plan existente
     */
    public TrainingPlan updatePlan(Long planId, TrainingPlanDTO planDto) {
        Optional<TrainingPlan> existing = planRepository.findById(planId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Plan no encontrado");
        }
        
        TrainingPlan toUpdate = existing.get();
        if (planDto.name != null) {
            toUpdate.setName(planDto.name);
        }
        if (planDto.description != null) {
            toUpdate.setDescription(planDto.description);
        }
        if (planDto.userId != null) {
            Optional<User> newUser = userRepository.findById(planDto.userId);
            if (newUser.isPresent()) {
                toUpdate.setUser(newUser.get());
            }
        }
        
        return planRepository.save(toUpdate);
    }
    
    /**
     * Actualizar plan del usuario autenticado (validar propiedad)
     */
    public TrainingPlan updateMyPlan(Long planId, TrainingPlanDTO planDto, Long currentUserId) {
        Optional<TrainingPlan> planOpt = planRepository.findById(planId);
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Plan no encontrado");
        }
        
        TrainingPlan plan = planOpt.get();
        
        // Validar que pertenece al usuario y no es template
        if (!currentUserId.equals(plan.getUser().getId()) || Boolean.TRUE.equals(plan.getIsTemplate())) {
            throw new IllegalArgumentException("No puedes modificar este plan");
        }
        
        if (planDto.name != null) {
            plan.setName(planDto.name);
        }
        if (planDto.description != null) {
            plan.setDescription(planDto.description);
        }
        
        return planRepository.save(plan);
    }
    
    /**
     * Eliminar un plan
     */
    public void deletePlan(Long planId) {
        if (!planRepository.existsById(planId)) {
            throw new IllegalArgumentException("Plan no encontrado");
        }
        planRepository.deleteById(planId);
    }
    
    // ==================== ARCHIVADO Y SNAPSHOTS ====================
    
    /**
     * Archivar un plan y crear snapshot de los ejercicios
     */
    public Map<String, Object> archivePlan(Long planId, String notes) {
        Optional<TrainingPlan> planOpt = planRepository.findById(planId);
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Plan no encontrado");
        }
        
        TrainingPlan plan = planOpt.get();
        
        if (Boolean.TRUE.equals(plan.getIsTemplate())) {
            throw new IllegalArgumentException("No se pueden archivar templates");
        }
        
        if (plan.getStatus() == TrainingPlan.PlanStatus.ARCHIVED) {
            throw new IllegalArgumentException("El plan ya está archivado");
        }
        
        // Serializar ejercicios actuales a JSON
        List<Exercise> exercises = exerciseRepository.findByTrainingPlanId(planId);
        String exercisesSnapshot = serializeExercisesToJson(exercises);
        
        // Crear histórico
        TrainingPlanHistory history = new TrainingPlanHistory();
        history.setTrainingPlan(plan);
        history.setUser(plan.getUser());
        history.setStartDate(plan.getStartDate());
        history.setEndDate(plan.getEndDate());
        history.setExercisesSnapshot(exercisesSnapshot);
        history.setNotes(notes != null ? notes : "");
        historyRepository.save(history);
        
        // Cambiar status a ARCHIVED
        plan.setStatus(TrainingPlan.PlanStatus.ARCHIVED);
        planRepository.save(plan);
        
        // Retornar información
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Plan archivado exitosamente");
        response.put("planId", planId);
        response.put("status", plan.getStatus().toString());
        response.put("snapshotId", history.getId());
        response.put("exercisesCount", exercises.size());
        response.put("archivedAt", history.getArchivedAt());
        
        return response;
    }
    
    /**
     * Serializar ejercicios a JSON para guardar en snapshot
     */
    private String serializeExercisesToJson(List<Exercise> exercises) {
        try {
            List<Map<String, Object>> exercisesList = new java.util.ArrayList<>();
            
            for (Exercise ex : exercises) {
                Map<String, Object> exData = new HashMap<>();
                exData.put("id", ex.getId());
                exData.put("name", ex.getName());
                exData.put("description", ex.getDescription());
                exData.put("sets", ex.getSets());
                exData.put("reps", ex.getReps());
                exData.put("weight", ex.getWeight());
                exData.put("videoUrl", ex.getVideoUrl());
                exData.put("memberComment", ex.getMemberComment());
                exData.put("trainerComment", ex.getTrainerComment());
                
                exercisesList.add(exData);
            }
            
            return objectMapper.writeValueAsString(exercisesList);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando ejercicios a JSON: " + e.getMessage(), e);
        }
    }
}
