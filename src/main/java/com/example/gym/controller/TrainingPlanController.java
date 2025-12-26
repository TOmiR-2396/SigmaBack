package com.example.gym.controller;

import com.example.gym.dto.AssignTemplateRequest;
import com.example.gym.dto.TrainingPlanDTO;
import com.example.gym.model.TrainingPlan;
import com.example.gym.model.TrainingPlanHistory;
import com.example.gym.model.User;
import com.example.gym.repository.TrainingPlanHistoryRepository;
import com.example.gym.repository.TrainingPlanRepository;
import com.example.gym.service.TrainingPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/plans")
public class TrainingPlanController {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingPlanController.class);
    
    @Autowired
    private TrainingPlanService trainingPlanService;
    @Autowired
    private TrainingPlanRepository planRepository;
    @Autowired
    private TrainingPlanHistoryRepository historyRepository;
    @Autowired
    private ObjectMapper objectMapper;

    // ================= PLANTILLAS =================
    
    @GetMapping("/templates")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getTemplates(Authentication auth) {
        List<TrainingPlan> templates = planRepository.findByIsTemplate(true);
        return ResponseEntity.ok(mapPlansToDTO(templates));
    }

    @PostMapping("/templates/create")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> createTemplate(@RequestBody TrainingPlanDTO planDto, Authentication auth) {
        try {
            TrainingPlan template = trainingPlanService.createTemplate(planDto);
            return ResponseEntity.ok(mapPlanToDTO(template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creando template: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error interno del servidor");
        }
    }

    @PutMapping("/templates/update/{templateId}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> updateTemplate(@PathVariable("templateId") Long templateId, 
                                          @RequestBody TrainingPlanDTO planDto, 
                                          Authentication auth) {
        try {
            TrainingPlan template = trainingPlanService.updateTemplate(templateId, planDto);
            return ResponseEntity.ok(mapPlanToDTO(template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error actualizando template: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error interno del servidor");
        }
    }

    @DeleteMapping("/templates/delete/{templateId}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> deleteTemplate(@PathVariable("templateId") Long templateId, Authentication auth) {
        try {
            trainingPlanService.deleteTemplate(templateId);
            return ResponseEntity.ok("Template deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> assignTemplateToUser(@RequestBody AssignTemplateRequest request) {
        try {
            TrainingPlan userPlan = trainingPlanService.assignTemplateToUser(request.getTemplateId(), request.getUserId());
            return ResponseEntity.ok(mapPlanToDTO(userPlan));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error asignando template: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error asignando template: " + e.getMessage());
        }
    }

    // ================= PLANES DE USUARIO =================
    
    @GetMapping("/my-plans")
    public ResponseEntity<?> getMyPlans(Authentication auth) {
        User current = (User) auth.getPrincipal();
        List<TrainingPlan> myPlans = planRepository.findByUserId(current.getId());
        return ResponseEntity.ok(mapPlansToDTO(myPlans));
    }

    @PutMapping("/my-plans/{planId}")
    public ResponseEntity<?> editMyPlan(@PathVariable("planId") Long planId, 
                                        @RequestBody TrainingPlanDTO planDto, 
                                        Authentication auth) {
        try {
            User current = (User) auth.getPrincipal();
            TrainingPlan plan = trainingPlanService.updateMyPlan(planId, planDto, current.getId());
            return ResponseEntity.ok(mapPlanToDTO(plan));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getAllAssigned(Authentication auth) {
        List<TrainingPlan> assignedPlans = planRepository.findByUserIsNotNull();
        return ResponseEntity.ok(mapPlansToDTO(assignedPlans));
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getUserPlans(@PathVariable("userId") Long userId) {
        List<TrainingPlan> userPlans = planRepository.findByUserId(userId);
        return ResponseEntity.ok(mapPlansToDTO(userPlans));
    }

    @GetMapping("/{planId}/progress")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getProgress(@PathVariable("planId") Long planId, Authentication auth) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("planId", planId);
        progress.put("sessions", new java.util.ArrayList<>());
        progress.put("lastUpdated", java.time.LocalDateTime.now());
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> createPlan(@RequestBody TrainingPlanDTO planDto, Authentication auth) {
        try {
            TrainingPlan plan = trainingPlanService.createPlan(planDto);
            return ResponseEntity.ok(mapPlanToDTO(plan));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> editPlan(@PathVariable("id") Long id, @RequestBody TrainingPlanDTO planDto, Authentication auth) {
        try {
            TrainingPlan plan = trainingPlanService.updatePlan(id, planDto);
            return ResponseEntity.ok(mapPlanToDTO(plan));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> deletePlan(@PathVariable("id") Long id, Authentication auth) {
        try {
            trainingPlanService.deletePlan(id);
            return ResponseEntity.ok("Plan deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= ARCHIVADO Y SNAPSHOTS =================
    
    @PostMapping("/{planId}/archive")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> archivePlan(@PathVariable("planId") Long planId, 
                                         @RequestParam(required = false) String notes,
                                         Authentication auth) {
        try {
            Map<String, Object> result = trainingPlanService.archivePlan(planId, notes);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error archivando plan: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error archivando el plan: " + e.getMessage());
        }
    }

    @GetMapping("/{planId}/history")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getPlanHistory(@PathVariable("planId") Long planId,
                                           Authentication auth) {
        try {
            Optional<TrainingPlan> planOpt = planRepository.findById(planId);
            if (planOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<TrainingPlanHistory> history = historyRepository.findByTrainingPlanIdOrderByArchivedAtDesc(planId);
            
            List<Map<String, Object>> historyDtos = new java.util.ArrayList<>();
            for (TrainingPlanHistory h : history) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", h.getId());
                dto.put("planId", h.getTrainingPlan().getId());
                dto.put("startDate", h.getStartDate());
                dto.put("endDate", h.getEndDate());
                dto.put("archivedAt", h.getArchivedAt());
                dto.put("notes", h.getNotes());
                
                try {
                    List<?> exercisesList = objectMapper.readValue(h.getExercisesSnapshot(), List.class);
                    dto.put("exercisesCount", exercisesList.size());
                } catch (Exception e) {
                    dto.put("exercisesCount", 0);
                }
                
                historyDtos.add(dto);
            }
            
            return ResponseEntity.ok(historyDtos);
            
        } catch (Exception e) {
            logger.error("Error obteniendo histórico: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error obteniendo histórico");
        }
    }

    @GetMapping("/history/{snapshotId}")
    @PreAuthorize("hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<?> getSnapshotDetails(@PathVariable("snapshotId") Long snapshotId,
                                               Authentication auth) {
        try {
            Optional<TrainingPlanHistory> historyOpt = historyRepository.findById(snapshotId);
            if (historyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TrainingPlanHistory history = historyOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", history.getId());
            response.put("planId", history.getTrainingPlan().getId());
            response.put("planName", history.getTrainingPlan().getName());
            response.put("userId", history.getUser().getId());
            response.put("userName", history.getUser().getFirstName() + " " + history.getUser().getLastName());
            response.put("startDate", history.getStartDate());
            response.put("endDate", history.getEndDate());
            response.put("archivedAt", history.getArchivedAt());
            response.put("notes", history.getNotes());
            
            try {
                List<?> exercisesList = objectMapper.readValue(history.getExercisesSnapshot(), List.class);
                response.put("exercises", exercisesList);
            } catch (Exception e) {
                response.put("exercises", new java.util.ArrayList<>());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error obteniendo snapshot: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error obteniendo snapshot");
        }
    }

    // ================= MAPEO DTO =================
    
    private TrainingPlanDTO mapPlanToDTO(TrainingPlan plan) {
        TrainingPlanDTO dto = new TrainingPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.description = plan.getDescription();
        dto.userId = plan.getUser() != null ? plan.getUser().getId() : null;
        dto.isTemplate = plan.getIsTemplate();
        return dto;
    }

    private List<TrainingPlanDTO> mapPlansToDTO(List<TrainingPlan> plans) {
        List<TrainingPlanDTO> dtos = new java.util.ArrayList<>();
        for (TrainingPlan plan : plans) {
            dtos.add(mapPlanToDTO(plan));
        }
        return dtos;
    }
}

