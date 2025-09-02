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
    // Endpoint para obtener la URL del video de un ejercicio
    @GetMapping("/video/{id}")
    public ResponseEntity<?> getExerciseVideoInfo(@PathVariable("id") Long id, Authentication auth) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Exercise exercise = exerciseOpt.get();
        // Construir respuesta JSON con info relevante
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("id", exercise.getId());
        resp.put("name", exercise.getName());
        resp.put("description", exercise.getDescription());
        resp.put("videoUrl", exercise.getVideoUrl()); // puede ser null
        return ResponseEntity.ok(resp);
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
            // Guardar el archivo en la carpeta 'videos' dentro del directorio del proyecto
            String uploadDir = System.getProperty("user.dir") + java.io.File.separator + "videos";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String filename = "exercise_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            java.io.File dest = new java.io.File(dir, filename);
            file.transferTo(dest);
            // Guardar la ruta relativa para servir el video
            String relativePath = "videos/" + filename;
            exercise.setVideoUrl(relativePath);
            exerciseRepository.save(exercise);
            return ResponseEntity.ok("Video uploaded and linked to exercise");
        } catch (Exception e) {
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
    public ResponseEntity<?> createExercise(@RequestBody Exercise exercise, @RequestParam Long planId, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can create exercises");
        }
        Optional<TrainingPlan> plan = planRepository.findById(planId);
        if (plan.isEmpty()) {
            return ResponseEntity.badRequest().body("Training plan not found");
        }
        exercise.setTrainingPlan(plan.get());
        return ResponseEntity.ok(exerciseRepository.save(exercise));
    }
}
