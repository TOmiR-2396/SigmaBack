package com.example.gym.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExerciseService {

    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @Autowired
    private TrainingPlanRepository planRepository;
    
    @Autowired
    private ExerciseProgressRepository progressRepository;

    /**
     * Obtener todos los ejercicios
     */
    public List<ExerciseDTO> getAllExercises() {
        List<Exercise> exercises = exerciseRepository.findAll();
        return exercises.stream()
            .map(this::mapExerciseToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener ejercicios por plan de entrenamiento
     */
    public List<ExerciseDTO> getExercisesByPlan(Long planId) {
        Optional<TrainingPlan> plan = planRepository.findById(planId);
        if (plan.isEmpty()) {
            throw new IllegalArgumentException("Plan de entrenamiento no encontrado");
        }
        
        List<Exercise> exercises = exerciseRepository.findByTrainingPlanId(planId);
        return exercises.stream()
            .map(this::mapExerciseToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener un ejercicio por ID
     */
    public ExerciseDTO getExerciseById(Long id) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
        }
        return mapExerciseToDTO(exerciseOpt.get());
    }

    /**
     * Subir video a un ejercicio (Miembros pueden subir a sus propios ejercicios)
     */
    public ExerciseDTO uploadVideo(Long exerciseId, MultipartFile file, User currentUser) throws IOException {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
        }

        Exercise exercise = exerciseOpt.get();

        // Validación: si es MEMBER, solo puede subir a ejercicios de su propio plan
        if (currentUser.getRole() == User.UserRole.MEMBER) {
            TrainingPlan plan = exercise.getTrainingPlan();
            if (plan == null || plan.getUser() == null || !currentUser.getId().equals(plan.getUser().getId())) {
                throw new IllegalArgumentException("Solo puedes subir videos a ejercicios de tu propio plan");
            }
        }
        // TRAINER y OWNER pueden subir a cualquier ejercicio
        // Aquí no restringimos

        // Validaciones de tamaño y tipo
        long maxBytes = 150L * 1024 * 1024; // 150 MB
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("El video excede 150 MB. Súbelo comprimido o baja la resolución.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("video/")) {
            throw new IllegalArgumentException("Solo se permiten archivos de video (mp4)");
        }

        String uploadDir = System.getProperty("user.dir") + File.separator + "videos";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        // Nombre con timestamp (para limpieza por antigüedad)
        String sanitizedName = file.getOriginalFilename() == null ? "video.mp4" 
            : file.getOriginalFilename().replaceAll("\\s+", "_");
        String filename = "exercise_" + exerciseId + "_" + System.currentTimeMillis() + "_" + sanitizedName;
        File dest = new File(dir, filename);
        file.transferTo(dest);

        String relativePath = "videos/" + filename;
        exercise.setVideoUrl(relativePath);
        exerciseRepository.save(exercise);

        // Limpieza de videos de más de 14 días
        cleanupOldVideos(dir.toPath(), 14);

        return mapExerciseToDTO(exercise);
    }

    /**
     * Crear ejercicio
     */
    public ExerciseDTO createExercise(ExerciseDTO exerciseDto, Long planId, User currentUser) {
        if (!canEdit(currentUser)) {
            throw new IllegalArgumentException("Solo TRAINER u OWNER pueden crear ejercicios");
        }

        Optional<TrainingPlan> plan = planRepository.findById(planId);
        if (plan.isEmpty()) {
            throw new IllegalArgumentException("Plan de entrenamiento no encontrado");
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
        
        return mapExerciseToDTO(saved);
    }

    /**
     * Actualizar ejercicio y capturar progresión
     */
    public ExerciseDTO updateExercise(Long id, ExerciseDTO exerciseDto, User currentUser) {
        if (!canEdit(currentUser)) {
            throw new IllegalArgumentException("Solo TRAINER u OWNER pueden actualizar ejercicios");
        }
        
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
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
        return mapExerciseToDTO(updated);
    }

    /**
     * Obtener histórico completo de progresión de un ejercicio
     */
    public List<ProgressHistoryDTO> getExerciseProgress(Long exerciseId) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
        }
        
        var history = progressRepository.findByExerciseIdOrderByRecordedAtDesc(exerciseId);
        if (history.isEmpty()) {
            return List.of();
        }
        
        return history.stream()
            .map(p -> new ProgressHistoryDTO(p.getWeight(), p.getSets(), p.getReps(), p.getRecordedAt()))
            .collect(Collectors.toList());
    }

    /**
     * Agregar/actualizar comentario de miembro
     */
    public ExerciseDTO addMemberComment(Long exerciseId, String comment, User currentUser) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
        }

        Exercise exercise = exerciseOpt.get();

        // Validación: si es MEMBER, solo puede comentar en su propio plan
        if (currentUser.getRole() == User.UserRole.MEMBER) {
            TrainingPlan plan = exercise.getTrainingPlan();
            if (plan == null || plan.getUser() == null || !currentUser.getId().equals(plan.getUser().getId())) {
                throw new IllegalArgumentException("Solo puedes comentar ejercicios de tu propio plan");
            }
        }

        if (comment != null && comment.length() > 2000) {
            throw new IllegalArgumentException("El comentario supera el máximo de 2000 caracteres");
        }

        exercise.setMemberComment(comment != null ? comment : "");
        exerciseRepository.save(exercise);
        return mapExerciseToDTO(exercise);
    }

    /**
     * Agregar/actualizar comentario de entrenador
     */
    public ExerciseDTO addTrainerComment(Long exerciseId, String comment, User currentUser) {
        if (!canEdit(currentUser)) {
            throw new IllegalArgumentException("Solo TRAINER u OWNER pueden agregar comentarios de entrenador");
        }

        Optional<Exercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
        }

        Exercise exercise = exerciseOpt.get();

        if (comment != null && comment.length() > 2000) {
            throw new IllegalArgumentException("El comentario supera el máximo de 2000 caracteres");
        }

        exercise.setTrainerComment(comment != null ? comment : "");
        exerciseRepository.save(exercise);
        return mapExerciseToDTO(exercise);
    }

    /**
     * Helper: verificar si el usuario puede editar
     */
    private boolean canEdit(User user) {
        return user.getRole() == User.UserRole.TRAINER || user.getRole() == User.UserRole.OWNER;
    }

    /**
     * Mapear Exercise a ExerciseDTO con progresión
     */
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
                .collect(Collectors.toList());
        }
        
        return dto;
    }

    /**
     * Eliminar ejercicio individual (borrado duro) con limpieza de video y progreso
     */
    public void deleteExercise(Long id, User currentUser) {
        if (!canEdit(currentUser)) {
            throw new IllegalArgumentException("Solo TRAINER u OWNER pueden eliminar ejercicios");
        }

        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Ejercicio no encontrado");
        }

        Exercise ex = exerciseOpt.get();

        // Borrar archivo de video si existe
        try {
            String videoUrl = ex.getVideoUrl();
            if (videoUrl != null && !videoUrl.isBlank()) {
                java.io.File f = new java.io.File(System.getProperty("user.dir"), videoUrl);
                if (!f.isAbsolute()) {
                    f = new java.io.File(System.getProperty("user.dir") + java.io.File.separator + videoUrl);
                }
                if (f.exists()) {
                    boolean deleted = f.delete();
                    if (!deleted) {
                        // Log local: no tenemos logger aquí, mantener silencioso
                    }
                }
            }
        } catch (Exception ignore) {}

        // Borrar progreso del ejercicio
        progressRepository.deleteByExerciseId(id);

        // Borrar el ejercicio
        exerciseRepository.deleteById(id);
    }

    /**
     * Elimina videos más antiguos que retentionDays en el directorio
     */
    private void cleanupOldVideos(Path dir, int retentionDays) {
        try {
            Instant threshold = Instant.now().minus(Duration.ofDays(retentionDays));
            Files.list(dir)
                .filter(p -> Files.isRegularFile(p))
                .forEach(p -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        if (attrs.creationTime().toInstant().isBefore(threshold)) {
                            Files.deleteIfExists(p);
                        }
                    } catch (Exception ignored) { }
                });
        } catch (Exception ignored) { }
    }
}
