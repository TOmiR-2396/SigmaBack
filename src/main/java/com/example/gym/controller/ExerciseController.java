package com.example.gym.controller;

import com.example.gym.model.Exercise;
import com.example.gym.model.ExerciseProgress;
import com.example.gym.model.TrainingPlan;
import com.example.gym.model.User;
import com.example.gym.repository.ExerciseRepository;
import com.example.gym.repository.ExerciseProgressRepository;
import com.example.gym.repository.TrainingPlanRepository;
import com.example.gym.dto.ExerciseDTO;
import com.example.gym.dto.ProgressHistoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private TrainingPlanRepository planRepository;
    @Autowired
    private ExerciseProgressRepository progressRepository;
    
    // Endpoint para obtener todos los ejercicios
    @GetMapping
    public ResponseEntity<?> getAllExercises(Authentication auth) {
        try {
            java.util.List<Exercise> exercises = exerciseRepository.findAll();
            java.util.List<ExerciseDTO> exerciseDTOs = exercises.stream()
                .map(this::mapExerciseToDTO)
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
            java.util.List<ExerciseDTO> exerciseDTOs = exercises.stream()
                .map(this::mapExerciseToDTO)
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
        return ResponseEntity.ok(mapExerciseToDTO(exercise));
    }

    // Endpoint para obtener un ejercicio por id (detalle con progreso)
    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseById(@PathVariable("id") Long id, Authentication auth) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapExerciseToDTO(exerciseOpt.get()));
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
            // Validaciones de tamaño y tipo
            long maxBytes = 150L * 1024 * 1024; // 150 MB
            if (file.getSize() > maxBytes) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("El video excede 150 MB. Súbelo comprimido o baja la resolución.");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("video/")) {
                return ResponseEntity.badRequest().body("Solo se permiten archivos de video (mp4)");
            }

            String uploadDir = System.getProperty("user.dir") + java.io.File.separator + "videos";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            // Nombre con timestamp (para limpieza por antigüedad)
            String sanitizedName = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename().replaceAll("\\s+", "_");
            String filename = "exercise_" + id + "_" + System.currentTimeMillis() + "_" + sanitizedName;
            java.io.File dest = new java.io.File(dir, filename);
            file.transferTo(dest);

            String relativePath = "videos/" + filename;
            exercise.setVideoUrl(relativePath);
            exerciseRepository.save(exercise);

            // Limpieza de videos de más de 14 días
            cleanupOldVideos(dir.toPath(), 14);

            return ResponseEntity.ok(mapExerciseToDTO(exercise));
        } catch (java.io.IOException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading video: " + e.getMessage());
        }
    }

    // Solo OWNER y TRAINER pueden crear ejercicios
    private boolean canEdit(User user) {
        return user.getRole() == User.UserRole.TRAINER || user.getRole() == User.UserRole.OWNER;
    }

    // Helper para mapear Exercise a ExerciseDTO con progresión
    private ExerciseDTO mapExerciseToDTO(Exercise exercise) {
        ExerciseDTO dto = new ExerciseDTO();
        dto.id = exercise.getId();
        dto.name = exercise.getName();
        dto.description = exercise.getDescription();
        dto.memberComment = exercise.getMemberComment();
        dto.trainerComment = exercise.getTrainerComment();
        dto.videoUrl = exercise.getVideoUrl();
        dto.trainingPlanId = exercise.getTrainingPlan() != null ? exercise.getTrainingPlan().getId() : null;
        dto.sets = exercise.getSets();
        dto.reps = exercise.getReps();
        dto.weight = exercise.getWeight();
        
        // Obtener último registro de progreso para calcular % mejora
        ExerciseProgress latest = progressRepository.findLatestByExerciseId(exercise.getId());
        if (latest != null) {
            dto.previousWeight = latest.getWeight();
            dto.lastUpdatedAt = latest.getRecordedAt();
            
            // Calcular porcentaje de mejora
            if (latest.getWeight() != null && exercise.getWeight() != null && latest.getWeight() > 0) {
                double percentage = ((exercise.getWeight() - latest.getWeight()) / latest.getWeight()) * 100;
                dto.progressPercentage = Math.round(percentage * 100.0) / 100.0; // 2 decimales
            }
        }
        
        // Obtener histórico (últimos 10 registros)
        var history = progressRepository.findByExerciseIdOrderByRecordedAtDesc(exercise.getId());
        if (!history.isEmpty()) {
            var limited = history.size() > 10 ? history.subList(0, 10) : history;
            dto.progressHistory = limited.stream()
                .map(p -> new ProgressHistoryDTO(p.getWeight(), p.getSets(), p.getReps(), p.getRecordedAt()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return dto;
    }

    // Elimina videos más antiguos que retentionDays en el directorio
    private void cleanupOldVideos(java.nio.file.Path dir, int retentionDays) {
        try {
            java.time.Instant threshold = java.time.Instant.now().minus(java.time.Duration.ofDays(retentionDays));
            java.nio.file.Files.list(dir)
                .filter(p -> java.nio.file.Files.isRegularFile(p))
                .forEach(p -> {
                    try {
                        java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(p, java.nio.file.attribute.BasicFileAttributes.class);
                        if (attrs.creationTime().toInstant().isBefore(threshold)) {
                            java.nio.file.Files.deleteIfExists(p);
                        }
                    } catch (Exception ignored) { }
                });
        } catch (Exception ignored) { }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createExercise(@RequestBody ExerciseDTO exerciseDto, @RequestParam Long planId, Authentication auth) {
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
        exercise.setMemberComment(exerciseDto.memberComment);
        exercise.setTrainerComment(exerciseDto.trainerComment);
        exercise.setTrainingPlan(plan.get());
        exercise.setVideoUrl(exerciseDto.videoUrl);
        exercise.setSets(exerciseDto.sets);
        exercise.setReps(exerciseDto.reps);
        exercise.setWeight(exerciseDto.weight);
        Exercise saved = exerciseRepository.save(exercise);
        
        // Guardar primer registro de progreso
        if (saved.getWeight() != null) {
            ExerciseProgress progress = new ExerciseProgress(saved, saved.getWeight(), saved.getSets(), saved.getReps());
            progressRepository.save(progress);
        }
        
        return ResponseEntity.ok(mapExerciseToDTO(saved));
    }
    
    // PUT /api/exercises/{id} - Actualizar ejercicio y capturar progresión
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExercise(@PathVariable Long id, @RequestBody ExerciseDTO exerciseDto, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can update exercises");
        }
        
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Exercise exercise = exerciseOpt.get();
        
        // Guardar registro de progreso ANTES de actualizar (si hay cambios en weight/sets/reps)
        if ((exerciseDto.weight != null && !exerciseDto.weight.equals(exercise.getWeight())) ||
            (exerciseDto.sets != null && !exerciseDto.sets.equals(exercise.getSets())) ||
            (exerciseDto.reps != null && !exerciseDto.reps.equals(exercise.getReps()))) {
            
            ExerciseProgress progress = new ExerciseProgress(
                exercise, 
                exercise.getWeight(), 
                exercise.getSets(), 
                exercise.getReps()
            );
            progressRepository.save(progress);
        }
        
        // Actualizar campos
        if (exerciseDto.name != null) exercise.setName(exerciseDto.name);
        if (exerciseDto.description != null) exercise.setDescription(exerciseDto.description);
        if (exerciseDto.memberComment != null) exercise.setMemberComment(exerciseDto.memberComment);
        if (exerciseDto.trainerComment != null) exercise.setTrainerComment(exerciseDto.trainerComment);
        if (exerciseDto.sets != null) exercise.setSets(exerciseDto.sets);
        if (exerciseDto.reps != null) exercise.setReps(exerciseDto.reps);
        if (exerciseDto.weight != null) exercise.setWeight(exerciseDto.weight);
        if (exerciseDto.videoUrl != null) exercise.setVideoUrl(exerciseDto.videoUrl);
        
        Exercise updated = exerciseRepository.save(exercise);
        return ResponseEntity.ok(mapExerciseToDTO(updated));
    }
    
    // GET /api/exercises/{id}/progress - Obtener histórico de progresión completo
    @GetMapping("/{id}/progress")
    public ResponseEntity<?> getExerciseProgress(@PathVariable Long id, Authentication auth) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var history = progressRepository.findByExerciseIdOrderByRecordedAtDesc(id);
        if (history.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        
        var progressList = history.stream()
            .map(p -> new ProgressHistoryDTO(p.getWeight(), p.getSets(), p.getReps(), p.getRecordedAt()))
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(progressList);
    }

    // PUT /api/exercises/{id}/comment/member - Miembro agrega/actualiza su comentario
    @PutMapping("/{id}/comment/member")
    public ResponseEntity<?> addMemberComment(@PathVariable Long id,
                                              @RequestBody java.util.Map<String, String> body,
                                              Authentication auth) {
        User current = (User) auth.getPrincipal();
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise exercise = exerciseOpt.get();

        // Validación: si es MEMBER, solo puede comentar en su propio plan
        if (current.getRole() == User.UserRole.MEMBER) {
            TrainingPlan plan = exercise.getTrainingPlan();
            if (plan == null || plan.getUser() == null || !current.getId().equals(plan.getUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Solo puedes comentar ejercicios de tu propio plan");
            }
        }

        String comment = body != null ? body.getOrDefault("comment", "") : "";
        if (comment.length() > 2000) {
            return ResponseEntity.badRequest().body("El comentario supera el máximo de 2000 caracteres");
        }
        exercise.setMemberComment(comment);
        exerciseRepository.save(exercise);
        return ResponseEntity.ok(mapExerciseToDTO(exercise));
    }

    // PUT /api/exercises/{id}/comment/trainer - Entrenador/Owner agrega/actualiza comentario del entrenador
    @PutMapping("/{id}/comment/trainer")
    public ResponseEntity<?> addTrainerComment(@PathVariable Long id,
                                               @RequestBody java.util.Map<String, String> body,
                                               Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo TRAINER u OWNER pueden agregar comentarios de entrenador");
        }

        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise exercise = exerciseOpt.get();

        String comment = body != null ? body.getOrDefault("comment", "") : "";
        if (comment.length() > 2000) {
            return ResponseEntity.badRequest().body("El comentario supera el máximo de 2000 caracteres");
        }
        exercise.setTrainerComment(comment);
        exerciseRepository.save(exercise);
        return ResponseEntity.ok(mapExerciseToDTO(exercise));
    }
}