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
    public ResponseEntity<?> createPlan(@RequestBody TrainingPlan plan, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can create plans");
        }
        // El usuario miembro debe existir
        Optional<User> member = userRepository.findById(plan.getUser().getId());
        if (member.isEmpty()) {
            return ResponseEntity.badRequest().body("Target user not found");
        }
        plan.setUser(member.get());
        return ResponseEntity.ok(planRepository.save(plan));
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editPlan(@PathVariable("id") Long id, @RequestBody TrainingPlan plan, Authentication auth) {
        User current = (User) auth.getPrincipal();
        if (!canEdit(current)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TRAINER or OWNER can edit plans");
        }
        Optional<TrainingPlan> existing = planRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TrainingPlan toUpdate = existing.get();
        // Solo actualiza los campos enviados en el body
        if (plan.getName() != null) {
            toUpdate.setName(plan.getName());
        }
        if (plan.getDescription() != null) {
            toUpdate.setDescription(plan.getDescription());
        }
        // Si se quiere cambiar el usuario asignado al plan
        if (plan.getUser() != null && plan.getUser().getId() != null) {
            Optional<User> newUser = userRepository.findById(plan.getUser().getId());
            if (newUser.isPresent()) {
                toUpdate.setUser(newUser.get());
            }
        }
        return ResponseEntity.ok(planRepository.save(toUpdate));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TrainingPlan>> getPlansForUser(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(planRepository.findByUserId(userId));
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
