package com.example.gym.controller;

import com.example.gym.dto.AssignTemplateRequest;
import com.example.gym.model.TrainingPlan;
import com.example.gym.model.User;
import com.example.gym.repository.TrainingPlanRepository;
import com.example.gym.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/plans")
public class TrainingPlanController {
    @Autowired
    private TrainingPlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;

    // GET /api/plans/templates - Obtener todas las plantillas
    @GetMapping("/templates")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getTemplates(Authentication auth) {
        List<TrainingPlan> templates = planRepository.findByIsTemplate(true);
        return ResponseEntity.ok(mapPlansToDTO(templates));
    }

    // POST /api/plans/templates/create - Crear nueva plantilla
    @PostMapping("/templates/create")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> createTemplate(@RequestBody com.example.gym.dto.TrainingPlanDTO planDto, Authentication auth) {
        // Validar que el nombre no sea null
        if (planDto.name == null || planDto.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre de la plantilla es requerido");
        }
        
        TrainingPlan template = new TrainingPlan();
        template.setName(planDto.name.trim());
        template.setDescription(planDto.description != null ? planDto.description.trim() : "");
        template.setIsTemplate(true);
        template.setUser(null); // Templates no tienen usuario asignado
        TrainingPlan saved = planRepository.save(template);
        return ResponseEntity.ok(mapPlanToDTO(saved));
    }

    // DELETE /api/plans/templates/delete/:templateId - Eliminar plantilla
    @DeleteMapping("/templates/delete/{templateId}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> deleteTemplate(@PathVariable("templateId") Long templateId, Authentication auth) {
        Optional<TrainingPlan> template = planRepository.findById(templateId);
        if (template.isEmpty() || !Boolean.TRUE.equals(template.get().getIsTemplate())) {
            return ResponseEntity.badRequest().body("Template not found");
        }
        planRepository.deleteById(templateId);
        return ResponseEntity.ok("Template deleted");
    }

    // POST /api/plans/assign - TRAINER/OWNER asigna template a usuario
    @PostMapping("/assign")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> assignTemplateToUser(@RequestBody AssignTemplateRequest request) {
        Long templateId = request.getTemplateId();
        Long userId = request.getUserId();
        
        try {
            // Buscar template
            Optional<TrainingPlan> templateOpt = planRepository.findById(templateId);
            if (templateOpt.isEmpty() || !Boolean.TRUE.equals(templateOpt.get().getIsTemplate())) {
                return ResponseEntity.badRequest().body("Template no encontrado");
            }
            
            // Buscar usuario
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Usuario no encontrado");
            }
            
            // Crear COPIA del template para el usuario
            TrainingPlan template = templateOpt.get();
            TrainingPlan userPlan = new TrainingPlan();
            userPlan.setName(template.getName() + " - " + userOpt.get().getFirstName());
            userPlan.setDescription(template.getDescription());
            userPlan.setUser(userOpt.get()); // Asignar al usuario
            userPlan.setIsTemplate(false); // NO es template, es plan de usuario
            
            TrainingPlan saved = planRepository.save(userPlan);
            return ResponseEntity.ok(mapPlanToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error asignando template: " + e.getMessage());
        }
    }    // GET /api/plans/my-plans - Usuario ve sus planes asignados
    @GetMapping("/my-plans")
    public ResponseEntity<?> getMyPlans(Authentication auth) {
        User current = (User) auth.getPrincipal();
        List<TrainingPlan> myPlans = planRepository.findByUserId(current.getId());
        return ResponseEntity.ok(mapPlansToDTO(myPlans));
    }

    // PUT /api/plans/my-plans/{planId} - Usuario modifica su plan asignado
    @PutMapping("/my-plans/{planId}")
    public ResponseEntity<?> editMyPlan(@PathVariable("planId") Long planId, 
                                        @RequestBody com.example.gym.dto.TrainingPlanDTO planDto, 
                                        Authentication auth) {
        User current = (User) auth.getPrincipal();
        Optional<TrainingPlan> planOpt = planRepository.findById(planId);
        
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TrainingPlan plan = planOpt.get();
        
        // Verificar que el plan pertenece al usuario actual y no es template
        if (!current.getId().equals(plan.getUser().getId()) || Boolean.TRUE.equals(plan.getIsTemplate())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes modificar este plan");
        }
        
        // Actualizar solo campos permitidos
        if (planDto.name != null) {
            plan.setName(planDto.name);
        }
        if (planDto.description != null) {
            plan.setDescription(planDto.description);
        }
        
        TrainingPlan saved = planRepository.save(plan);
        return ResponseEntity.ok(mapPlanToDTO(saved));
    }

    // GET /api/plans/assigned - Obtener todos los planes asignados
    @GetMapping("/assigned")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getAllAssigned(Authentication auth) {
        List<TrainingPlan> assignedPlans = planRepository.findByUserIsNotNull();
        return ResponseEntity.ok(mapPlansToDTO(assignedPlans));
    }
    
    // GET /api/plans/user/{userId} - TRAINER/OWNER ve planes de un usuario específico
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getUserPlans(@PathVariable("userId") Long userId) {
        List<TrainingPlan> userPlans = planRepository.findByUserId(userId);
        return ResponseEntity.ok(mapPlansToDTO(userPlans));
    }

    // GET /api/plans/:planId/progress - Obtener historial de progreso (placeholder)
    @GetMapping("/{planId}/progress")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getProgress(@PathVariable("planId") Long planId, Authentication auth) {
        // Placeholder - retorna estructura básica
        java.util.Map<String, Object> progress = new java.util.HashMap<>();
        progress.put("planId", planId);
        progress.put("sessions", new java.util.ArrayList<>());
        progress.put("lastUpdated", java.time.LocalDateTime.now());
        return ResponseEntity.ok(progress);
    }

    private com.example.gym.dto.TrainingPlanDTO mapPlanToDTO(TrainingPlan plan) {
        com.example.gym.dto.TrainingPlanDTO dto = new com.example.gym.dto.TrainingPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.description = plan.getDescription();
        dto.userId = plan.getUser() != null ? plan.getUser().getId() : null;
        dto.isTemplate = plan.getIsTemplate();
        return dto;
    }

    private java.util.List<com.example.gym.dto.TrainingPlanDTO> mapPlansToDTO(List<TrainingPlan> plans) {
        java.util.List<com.example.gym.dto.TrainingPlanDTO> dtos = new java.util.ArrayList<>();
        for (TrainingPlan plan : plans) {
            dtos.add(mapPlanToDTO(plan));
        }
        return dtos;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> createPlan(@RequestBody com.example.gym.dto.TrainingPlanDTO planDto, Authentication auth) {
        // Validar que los campos requeridos no sean null
        if (planDto.name == null || planDto.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre del plan es requerido");
        }
        if (planDto.userId == null) {
            return ResponseEntity.badRequest().body("El ID del usuario es requerido");
        }
        
        Optional<User> member = userRepository.findById(planDto.userId);
        if (member.isEmpty()) {
            return ResponseEntity.badRequest().body("Target user not found");
        }
        TrainingPlan plan = new TrainingPlan();
        plan.setName(planDto.name.trim());
        plan.setDescription(planDto.description != null ? planDto.description.trim() : "");
        plan.setIsTemplate(false);
        plan.setUser(member.get());
        TrainingPlan saved = planRepository.save(plan);
        return ResponseEntity.ok(mapPlanToDTO(saved));
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> editPlan(@PathVariable("id") Long id, @RequestBody com.example.gym.dto.TrainingPlanDTO planDto, Authentication auth) {
        Optional<TrainingPlan> existing = planRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
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
        TrainingPlan saved = planRepository.save(toUpdate);
        return ResponseEntity.ok(mapPlanToDTO(saved));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> deletePlan(@PathVariable("id") Long id, Authentication auth) {
        if (!planRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        planRepository.deleteById(id);
        return ResponseEntity.ok("Plan deleted");
    }
}
