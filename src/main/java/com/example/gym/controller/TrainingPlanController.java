package com.example.gym.controller;

import com.example.gym.model.TrainingPlan;
import com.example.gym.model.User;
import com.example.gym.repository.TrainingPlanRepository;
import com.example.gym.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Solo OWNER y TRAINER pueden crear o editar planes
    private boolean canEdit(User user) {
        return user.getRole() == User.UserRole.TRAINER || user.getRole() == User.UserRole.OWNER;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPlan(@RequestBody com.example.gym.dto.TrainingPlanDTO planDto, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can create plans");
        }
        Optional<User> member = userRepository.findById(planDto.userId);
        if (member.isEmpty()) {
            return ResponseEntity.badRequest().body("Target user not found");
        }
        TrainingPlan plan = new TrainingPlan();
        plan.setName(planDto.name);
        plan.setDescription(planDto.description);
        plan.setUser(member.get());
        TrainingPlan saved = planRepository.save(plan);
        com.example.gym.dto.TrainingPlanDTO dto = new com.example.gym.dto.TrainingPlanDTO();
        dto.id = saved.getId();
        dto.name = saved.getName();
        dto.description = saved.getDescription();
        dto.userId = saved.getUser() != null ? saved.getUser().getId() : null;
        // Exercises omitted for brevity
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editPlan(@PathVariable("id") Long id, @RequestBody com.example.gym.dto.TrainingPlanDTO planDto, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can edit plans");
        }
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
        com.example.gym.dto.TrainingPlanDTO dto = new com.example.gym.dto.TrainingPlanDTO();
        dto.id = saved.getId();
        dto.name = saved.getName();
        dto.description = saved.getDescription();
        dto.userId = saved.getUser() != null ? saved.getUser().getId() : null;
        // Exercises omitted for brevity
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<java.util.List<com.example.gym.dto.TrainingPlanDTO>> getPlansForUser(@PathVariable("userId") Long userId) {
        List<TrainingPlan> plans = planRepository.findByUserId(userId);
        java.util.List<com.example.gym.dto.TrainingPlanDTO> dtos = new java.util.ArrayList<>();
        for (TrainingPlan plan : plans) {
            com.example.gym.dto.TrainingPlanDTO dto = new com.example.gym.dto.TrainingPlanDTO();
            dto.id = plan.getId();
            dto.name = plan.getName();
            dto.description = plan.getDescription();
            dto.userId = plan.getUser() != null ? plan.getUser().getId() : null;
            // Exercises omitted for brevity
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable("id") Long id, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can delete plans");
        }
        if (!planRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        planRepository.deleteById(id);
        return ResponseEntity.ok("Plan deleted");
    }
}
