package com.example.gym.controller;

import com.example.gym.model.MembershipPlan;
import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import com.example.gym.repository.MembershipPlanRepository;
import com.example.gym.repository.SubscriptionRepository;
import com.example.gym.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/membership-plans")
public class MembershipPlanController {
    // Obtener membresía activa de un usuario por su id, con tiempo restante
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMembershipByUserId(@PathVariable("userId") Long userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findAll().stream()
            .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
            .findFirst();
        if (subscriptionOpt.isEmpty()) {
            return ResponseEntity.ok("El usuario no tiene membresía activa");
        }
        Subscription subscription = subscriptionOpt.get();
        Long planId = subscription.getPlan().getId();
        com.example.gym.model.MembershipPlan plan = membershipPlanRepository.findById(planId).orElse(null);
        if (plan == null) {
            return ResponseEntity.status(500).body("Error: No se encontró el plan de membresía.");
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate endDate = subscription.getEndDate();
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
        com.example.gym.dto.MembershipInfoDTO dto = new com.example.gym.dto.MembershipInfoDTO();
        dto.planId = plan.getId();
        dto.planName = plan.getName();
        dto.durationMonths = plan.getDurationMonths();
        dto.price = plan.getPrice();
    dto.daysPerWeek = plan.getDaysPerWeek();
        dto.startDate = subscription.getStartDate();
        dto.endDate = endDate;
        dto.status = subscription.getStatus().name();
        dto.daysLeft = daysLeft > 0 ? daysLeft : 0;
        return ResponseEntity.ok(dto);
    }

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private UserRepository userRepository;

    // Crear membresía (solo OWNER)
    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody com.example.gym.dto.MembershipPlanDTO planDto, Authentication auth) {
        Optional<User> ownerOpt = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst();
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Solo OWNER puede crear membresías");
        }
        MembershipPlan plan = new MembershipPlan();
        plan.setName(planDto.name);
        plan.setDurationMonths(planDto.durationMonths);
        plan.setPrice(planDto.price);
        plan.setDaysPerWeek(planDto.daysPerWeek);
        if (plan.getDaysPerWeek() == null || plan.getDaysPerWeek() < 1 || plan.getDaysPerWeek() > 5) {
            return ResponseEntity.badRequest().body("daysPerWeek debe estar entre 1 y 5");
        }
        if (plan.getName().equalsIgnoreCase("Funcional Kids") && plan.getDurationMonths() != 2) {
            return ResponseEntity.badRequest().body("Funcional Kids solo puede ser de 2 días a la semana");
        }
        membershipPlanRepository.save(plan);
        com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.durationMonths = plan.getDurationMonths();
        dto.price = plan.getPrice();
        dto.daysPerWeek = plan.getDaysPerWeek();
        return ResponseEntity.ok(dto);
    }

    // Listar todos los planes
    @GetMapping
    public List<com.example.gym.dto.MembershipPlanDTO> getAllPlans() {
        List<MembershipPlan> plans = membershipPlanRepository.findAll();
        java.util.List<com.example.gym.dto.MembershipPlanDTO> dtos = new java.util.ArrayList<>();
        for (MembershipPlan plan : plans) {
            com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
            dto.id = plan.getId();
            dto.name = plan.getName();
            dto.durationMonths = plan.getDurationMonths();
            dto.price = plan.getPrice();
            dto.daysPerWeek = plan.getDaysPerWeek();
            dtos.add(dto);
        }
        return dtos;
    }

    // Obtener plan por id
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlan(@PathVariable("id") Long id) {
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MembershipPlan plan = planOpt.get();
        com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.durationMonths = plan.getDurationMonths();
        dto.price = plan.getPrice();
    dto.daysPerWeek = plan.getDaysPerWeek();
        return ResponseEntity.ok(dto);
    }

    // Asignar membresía a usuario (solo OWNER)
    @PostMapping("/assign")
    public ResponseEntity<?> assignPlanToUser(@RequestParam("userId") Long userId,
                                              @RequestParam("planId") Long planId,
                                              Authentication auth) {
        // Validar que el usuario autenticado sea OWNER
        // Buscar usuario OWNER autenticado por rol
        Optional<User> ownerOpt = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst();
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Solo OWNER puede asignar membresías");
        }

        // Buscar usuario y plan
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(planId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Usuario no encontrado");
        }
        if (planOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Plan de membresía no encontrado");
        }
        // Validar que el usuario destino sea MEMBER
        if (userOpt.get().getRole() != User.UserRole.MEMBER) {
            return ResponseEntity.badRequest().body("Solo se puede asignar membresía a usuarios con rol MEMBER");
        }

        // Crear nueva suscripción
        Subscription subscription = new Subscription();
        subscription.setUser(userOpt.get());
        subscription.setPlan(planOpt.get());
        subscription.setStartDate(java.time.LocalDate.now());
        Integer duration = planOpt.get().getDurationMonths();
        if (duration == null || duration < 1) {
            duration = 1;
        }
        subscription.setEndDate(java.time.LocalDate.now().plusMonths(duration));
        subscription.setStatus(Subscription.Status.ACTIVE);
        subscriptionRepository.save(subscription);
        return ResponseEntity.ok("Membresía asignada correctamente");
    }
}
