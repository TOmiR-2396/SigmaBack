package com.example.gym.controller;

import com.example.gym.model.Exercise;
import com.example.gym.model.TrainingPlan;
import com.example.gym.model.User;
import com.example.gym.repository.ExerciseRepository;
import com.example.gym.repository.TrainingPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    
    // Endpoint para obtener todos los ejercicios
    @GetMapping
    public ResponseEntity<?> getAllExercises(Authentication auth) {
        try {
            java.util.List<Exercise> exercises = exerciseRepository.findAll();
            java.util.List<com.example.gym.dto.ExerciseDTO> exerciseDTOs = exercises.stream()
                .map(exercise -> {
                    com.example.gym.dto.ExerciseDTO dto = new com.example.gym.dto.ExerciseDTO();
                    dto.id = exercise.getId();
                    dto.name = exercise.getName();
                    dto.description = exercise.getDescription();
                    dto.videoUrl = exercise.getVideoUrl();
                    dto.trainingPlanId = exercise.getTrainingPlan() != null ? exercise.getTrainingPlan().getId() : null;
                    dto.sets = exercise.getSets();
                    dto.reps = exercise.getReps();
                    dto.weight = exercise.getWeight();
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(exerciseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving exercises: " + e.getMessage());
        }
    }
    
    // Endpoint para obtener ejercicios por plan de entrenamiento
    @GetMapping("/by-plan/{planId}")
    public ResponseEntity<?> getExercisesByPlan(@PathVariable Long planId, Authentication auth) {
        try {
            Optional<TrainingPlan> plan = planRepository.findById(planId);
            if (plan.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            java.util.List<Exercise> exercises = exerciseRepository.findByTrainingPlanId(planId);
            java.util.List<com.example.gym.dto.ExerciseDTO> exerciseDTOs = exercises.stream()
                .map(exercise -> {
                    com.example.gym.dto.ExerciseDTO dto = new com.example.gym.dto.ExerciseDTO();
                    dto.id = exercise.getId();
                    dto.name = exercise.getName();
                    dto.description = exercise.getDescription();
                    dto.videoUrl = exercise.getVideoUrl();
                    dto.trainingPlanId = exercise.getTrainingPlan() != null ? exercise.getTrainingPlan().getId() : null;
                    dto.sets = exercise.getSets();
                    dto.reps = exercise.getReps();
                    dto.weight = exercise.getWeight();
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(exerciseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving exercises for plan: " + e.getMessage());
        }
    }

    // Endpoint para obtener la URL del video de un ejercicio
    @GetMapping("/video/{id}")
    public ResponseEntity<?> getExerciseVideoInfo(@PathVariable("id") Long id, Authentication auth) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise exercise = exerciseOpt.get();
        com.example.gym.dto.ExerciseDTO dto = new com.example.gym.dto.ExerciseDTO();
        dto.id = exercise.getId();
        dto.name = exercise.getName();
        dto.description = exercise.getDescription();
        dto.videoUrl = exercise.getVideoUrl();
        dto.trainingPlanId = exercise.getTrainingPlan() != null ? exercise.getTrainingPlan().getId() : null;
        dto.sets = exercise.getSets();
        dto.reps = exercise.getReps();
        dto.weight = exercise.getWeight();
        return ResponseEntity.ok(dto);
    }
    // Endpoint para subir video y asociarlo a un ejercicio
    @PostMapping("/upload-video/{id}")
    public ResponseEntity<?> uploadVideo(@PathVariable("id") Long id,
                                         @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                         Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can upload videos");
        }
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise exercise = exerciseOpt.get();
        try {
            String uploadDir = System.getProperty("user.dir") + java.io.File.separator + "videos";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String filename = "exercise_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            java.io.File dest = new java.io.File(dir, filename);
            file.transferTo(dest);
            String relativePath = "videos/" + filename;
            exercise.setVideoUrl(relativePath);
            exerciseRepository.save(exercise);
            com.example.gym.dto.ExerciseDTO dto = new com.example.gym.dto.ExerciseDTO();
            dto.id = exercise.getId();
            dto.name = exercise.getName();
            dto.description = exercise.getDescription();
            dto.videoUrl = exercise.getVideoUrl();
            dto.trainingPlanId = exercise.getTrainingPlan() != null ? exercise.getTrainingPlan().getId() : null;
            dto.sets = exercise.getSets();
            dto.reps = exercise.getReps();
            dto.weight = exercise.getWeight();
            return ResponseEntity.ok(dto);
        } catch (java.io.IOException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading video: " + e.getMessage());
        }
    }
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private TrainingPlanRepository planRepository;

    // Solo OWNER y TRAINER pueden crear ejercicios
    private boolean canEdit(User user) {
        return user.getRole() == User.UserRole.TRAINER || user.getRole() == User.UserRole.OWNER;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createExercise(@RequestBody com.example.gym.dto.ExerciseDTO exerciseDto, @RequestParam Long planId, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can create exercises");
        }
        Optional<TrainingPlan> plan = planRepository.findById(planId);
        if (plan.isEmpty()) {
            return ResponseEntity.badRequest().body("Training plan not found");
        }
        Exercise exercise = new Exercise();
        exercise.setName(exerciseDto.name);
        exercise.setDescription(exerciseDto.description);
        exercise.setTrainingPlan(plan.get());
        exercise.setVideoUrl(exerciseDto.videoUrl);
        exercise.setSets(exerciseDto.sets);
        exercise.setReps(exerciseDto.reps);
        exercise.setWeight(exerciseDto.weight);
        Exercise saved = exerciseRepository.save(exercise);
        com.example.gym.dto.ExerciseDTO dto = new com.example.gym.dto.ExerciseDTO();
        dto.id = saved.getId();
        dto.name = saved.getName();
        dto.description = saved.getDescription();
        dto.videoUrl = saved.getVideoUrl();
        dto.trainingPlanId = saved.getTrainingPlan() != null ? saved.getTrainingPlan().getId() : null;
        dto.sets = saved.getSets();
        dto.reps = saved.getReps();
        dto.weight = saved.getWeight();
        return ResponseEntity.ok(dto);
    }
}
