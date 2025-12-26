package com.example.gym.controller;

import com.example.gym.model.User;
import com.example.gym.service.ExerciseService;
import com.example.gym.dto.ExerciseDTO;
import com.example.gym.dto.ProgressHistoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    
    @Autowired
    private ExerciseService exerciseService;
    
    // Endpoint para obtener todos los ejercicios
    @GetMapping
    public ResponseEntity<?> getAllExercises(Authentication auth) {
        try {
            List<ExerciseDTO> exerciseDTOs = exerciseService.getAllExercises();
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
            List<ExerciseDTO> exerciseDTOs = exerciseService.getExercisesByPlan(planId);
            return ResponseEntity.ok(exerciseDTOs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving exercises for plan: " + e.getMessage());
        }
    }

    // Endpoint para obtener la URL del video de un ejercicio
    @GetMapping("/video/{id}")
    public ResponseEntity<?> getExerciseVideoInfo(@PathVariable("id") Long id, Authentication auth) {
        try {
            ExerciseDTO dto = exerciseService.getExerciseById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para obtener un ejercicio por id (detalle con progreso)
    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseById(@PathVariable("id") Long id, Authentication auth) {
        try {
            ExerciseDTO dto = exerciseService.getExerciseById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para subir video y asociarlo a un ejercicio
    @PostMapping("/upload-video/{id}")
    public ResponseEntity<?> uploadVideo(@PathVariable("id") Long id,
                                         @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                         Authentication auth) {
        User current = (User) auth.getPrincipal();
        try {
            ExerciseDTO dto = exerciseService.uploadVideo(id, file, current);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading video: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createExercise(@RequestBody ExerciseDTO exerciseDto, @RequestParam Long planId, Authentication auth) {
        User current = (User) auth.getPrincipal();
        try {
            ExerciseDTO dto = exerciseService.createExercise(exerciseDto, planId, current);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // PUT /api/exercises/{id} - Actualizar ejercicio y capturar progresión
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExercise(@PathVariable Long id, @RequestBody ExerciseDTO exerciseDto, Authentication auth) {
        User current = (User) auth.getPrincipal();
        try {
            ExerciseDTO dto = exerciseService.updateExercise(id, exerciseDto, current);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // GET /api/exercises/{id}/progress - Obtener histórico de progresión completo
    @GetMapping("/{id}/progress")
    public ResponseEntity<?> getExerciseProgress(@PathVariable Long id, Authentication auth) {
        try {
            List<ProgressHistoryDTO> progressList = exerciseService.getExerciseProgress(id);
            return ResponseEntity.ok(progressList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PUT /api/exercises/{id}/comment/member - Miembro agrega/actualiza su comentario
    @PutMapping("/{id}/comment/member")
    public ResponseEntity<?> addMemberComment(@PathVariable Long id,
                                              @RequestBody java.util.Map<String, String> body,
                                              Authentication auth) {
        User current = (User) auth.getPrincipal();
        try {
            String comment = body != null ? body.getOrDefault("comment", "") : "";
            ExerciseDTO dto = exerciseService.addMemberComment(id, comment, current);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Solo puedes comentar")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/exercises/{id}/comment/trainer - Entrenador/Owner agrega/actualiza comentario del entrenador
    @PutMapping("/{id}/comment/trainer")
    public ResponseEntity<?> addTrainerComment(@PathVariable Long id,
                                               @RequestBody java.util.Map<String, String> body,
                                               Authentication auth) {
        User current = (User) auth.getPrincipal();
        try {
            String comment = body != null ? body.getOrDefault("comment", "") : "";
            ExerciseDTO dto = exerciseService.addTrainerComment(id, comment, current);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}