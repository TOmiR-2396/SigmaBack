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
@RequestMapping({"/membership-plans", "/api/auth", "/api"})
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
        dto.setPlanId(plan.getId());
        dto.setPlanName(plan.getName());
        dto.setDurationMonths(plan.getDurationMonths());
        dto.setPrice(plan.getPrice());
        dto.setDaysPerWeek(plan.getDaysPerWeek());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(endDate);
        dto.setStatus(subscription.getStatus().name());
        dto.setDaysLeft(daysLeft > 0 ? daysLeft : 0);
        return ResponseEntity.ok(dto);
    }

    // ================= Verificar Estado de Membresía (para el frontend) =================
    @GetMapping("/user/{userId}/membership-status")
    public ResponseEntity<?> checkUserMembershipStatus(@PathVariable Long userId, Authentication authentication) {
        try {
            // Si no hay autenticación, retornar error
            if (authentication == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token de autenticación requerido");
            }
            
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userRepository.findByEmail(currentUserEmail);
            
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            }
            
            User currentUser = currentUserOpt.get();
            
            // Verificar permisos: 
            // - OWNER y TRAINER pueden ver cualquier usuario
            // - MEMBER solo puede ver su propia información
            boolean isOwnerOrTrainer = currentUser.getRole() == User.UserRole.OWNER || 
                                      currentUser.getRole() == User.UserRole.TRAINER;
            
            boolean isSelfRequest = currentUser.getId().equals(userId);
            
            if (!isOwnerOrTrainer && !isSelfRequest) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("No tienes permisos para ver esta información");
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            
            // Buscar suscripción activa
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
                .findFirst();

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("userId", user.getId());
            response.put("userEmail", user.getEmail());
            response.put("userName", user.getFirstName() + " " + user.getLastName());
            response.put("userStatus", user.getStatus());

            if (subscriptionOpt.isPresent()) {
                Subscription subscription = subscriptionOpt.get();
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate endDate = subscription.getEndDate();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
                
                // Verificar si la membresía ha vencido
                if (daysLeft < 0) {
                    // Membresía vencida - desactivar usuario y suscripción
                    user.setStatus(User.UserStatus.INACTIVE);
                    subscription.setStatus(Subscription.Status.EXPIRED);
                    userRepository.save(user);
                    subscriptionRepository.save(subscription);
                    
                    response.put("membershipStatus", "EXPIRED");
                    response.put("message", "Membresía vencida. Usuario desactivado automáticamente.");
                } else {
                    response.put("membershipStatus", "ACTIVE");
                    
                    // Obtener el plan de manera segura
                    Optional<com.example.gym.model.MembershipPlan> planOpt = membershipPlanRepository.findById(subscription.getPlan().getId());
                    if (planOpt.isPresent()) {
                        response.put("planName", planOpt.get().getName());
                    } else {
                        response.put("planName", "Plan no disponible");
                    }
                    
                    response.put("startDate", subscription.getStartDate());
                    response.put("endDate", endDate);
                    response.put("daysLeft", daysLeft);
                }
            } else {
                response.put("membershipStatus", "NO_MEMBERSHIP");
                response.put("message", "Usuario sin membresía activa");
            }

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al verificar estado de membresía: " + e.getMessage());
        }
    }

    // ================= Información de Membresía Mejorada =================
    @GetMapping("/membership-info-enhanced") 
    public ResponseEntity<?> getMembershipInfoEnhanced(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body("Token de autenticación requerido");
            }
            
            String userEmail = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body("Usuario no encontrado");
            }
            
            User user = userOpt.get();
            
            // Buscar suscripción activa del usuario autenticado
            Optional<Subscription> activeSubscriptionOpt = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) && s.getStatus() == Subscription.Status.ACTIVE)
                .findFirst();
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            
            // Información básica del usuario
            response.put("userId", user.getId());
            response.put("userEmail", user.getEmail());
            response.put("userName", user.getFirstName() + " " + user.getLastName());
            response.put("userStatus", user.getStatus());
            response.put("userRole", user.getRole());
            
            if (activeSubscriptionOpt.isPresent()) {
                Subscription subscription = activeSubscriptionOpt.get();
                MembershipPlan plan = subscription.getPlan();
                
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate startDate = subscription.getStartDate();
                java.time.LocalDate endDate = subscription.getEndDate();
                
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
                long daysUsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
                
                // Información de la membresía
                response.put("hasMembership", true);
                response.put("membershipStatus", "ACTIVE");
                
                // Información del plan
                response.put("planId", plan.getId());
                response.put("planName", plan.getName());
                response.put("planPrice", plan.getPrice());
                response.put("planDaysPerWeek", plan.getDaysPerWeek());
                response.put("planDurationMonths", plan.getDurationMonths());
                
                // Información de fechas
                response.put("startDate", startDate);
                response.put("endDate", endDate);
                response.put("daysLeft", Math.max(0, daysLeft));
                response.put("daysUsed", Math.max(0, daysUsed));
                response.put("totalDays", totalDays);
                
                // Estadísticas mejoradas
                double progressPercentage = totalDays > 0 ? (double) daysUsed / totalDays * 100 : 0;
                response.put("progressPercentage", Math.round(progressPercentage * 100.0) / 100.0);
                
                // Estado de la membresía
                if (daysLeft < 0) {
                    response.put("membershipStatus", "EXPIRED");
                    response.put("isExpired", true);
                    response.put("daysExpired", Math.abs(daysLeft));
                } else if (daysLeft <= 7) {
                    response.put("isExpiringSoon", true);
                    response.put("expirationWarning", "Tu membresía expira en " + daysLeft + " días");
                } else {
                    response.put("isExpiringSoon", false);
                }
                
                response.put("isExpired", daysLeft < 0);
                
                // Información adicional
                double dailyCost = plan.getPrice().doubleValue() / totalDays;
                response.put("costPerDay", Math.round(dailyCost * 100.0) / 100.0);
                
                // Calcular semanas restantes aproximadas
                long weeksLeft = daysLeft / 7;
                response.put("weeksLeft", Math.max(0, weeksLeft));
                
            } else {
                // Usuario sin membresía activa
                response.put("hasMembership", false);
                response.put("membershipStatus", "NO_MEMBERSHIP");
                response.put("message", "No tienes una membresía activa");
                response.put("isExpired", false);
                response.put("isExpiringSoon", false);
                
                // Buscar membresías expiradas o canceladas
                List<Subscription> pastSubscriptions = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(user.getId()) && 
                                (s.getStatus() == Subscription.Status.EXPIRED || 
                                 s.getStatus() == Subscription.Status.CANCELED))
                    .sorted((s1, s2) -> s2.getEndDate().compareTo(s1.getEndDate())) // Más reciente primero
                    .collect(java.util.stream.Collectors.toList());
                
                if (!pastSubscriptions.isEmpty()) {
                    Subscription lastSubscription = pastSubscriptions.get(0);
                    response.put("hadPreviousMembership", true);
                    response.put("lastMembershipEndDate", lastSubscription.getEndDate());
                    response.put("lastMembershipPlan", lastSubscription.getPlan().getName());
                } else {
                    response.put("hadPreviousMembership", false);
                }
            }
            
            // Información de planes disponibles (para renovación/compra)
            List<MembershipPlan> availablePlans = membershipPlanRepository.findAll();
            java.util.List<java.util.Map<String, Object>> plansInfo = new java.util.ArrayList<>();
            
            for (MembershipPlan plan : availablePlans) {
                java.util.Map<String, Object> planInfo = new java.util.HashMap<>();
                planInfo.put("id", plan.getId());
                planInfo.put("name", plan.getName());
                planInfo.put("price", plan.getPrice());
                planInfo.put("daysPerWeek", plan.getDaysPerWeek());
                planInfo.put("durationMonths", plan.getDurationMonths());
                plansInfo.add(planInfo);
            }
            response.put("availablePlans", plansInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al obtener información de membresía: " + e.getMessage());
        }
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

    // Editar membresía (solo OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable("id") Long id, 
                                        @RequestBody com.example.gym.dto.MembershipPlanDTO planDto, 
                                        Authentication auth) {
        // Validar que el usuario autenticado sea OWNER
        Optional<User> ownerOpt = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst();
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Solo OWNER puede editar membresías");
        }

        // Buscar el plan existente
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MembershipPlan plan = planOpt.get();
        
        // Validar datos de entrada
        if (planDto.daysPerWeek == null || planDto.daysPerWeek < 1 || planDto.daysPerWeek > 5) {
            return ResponseEntity.badRequest().body("daysPerWeek debe estar entre 1 y 5");
        }
        
        if (planDto.name != null && planDto.name.equalsIgnoreCase("Funcional Kids") && 
            planDto.durationMonths != null && planDto.durationMonths != 2) {
            return ResponseEntity.badRequest().body("Funcional Kids solo puede ser de 2 días a la semana");
        }

        // Actualizar campos
        if (planDto.name != null && !planDto.name.trim().isEmpty()) {
            plan.setName(planDto.name);
        }
        if (planDto.durationMonths != null && planDto.durationMonths > 0) {
            plan.setDurationMonths(planDto.durationMonths);
        }
        if (planDto.price != null && planDto.price.doubleValue() > 0) {
            plan.setPrice(planDto.price);
        }
        if (planDto.daysPerWeek != null) {
            plan.setDaysPerWeek(planDto.daysPerWeek);
        }

        // Guardar cambios
        membershipPlanRepository.save(plan);

        // Crear DTO de respuesta
        com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.durationMonths = plan.getDurationMonths();
        dto.price = plan.getPrice();
        dto.daysPerWeek = plan.getDaysPerWeek();
        
        return ResponseEntity.ok(dto);
    }

    // Eliminar membresía (solo OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable("id") Long id, Authentication auth) {
        // Validar que el usuario autenticado sea OWNER
        Optional<User> ownerOpt = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst();
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Solo OWNER puede eliminar membresías");
        }

        // Buscar el plan
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MembershipPlan plan = planOpt.get();

        // Verificar si hay suscripciones activas asociadas a este plan
        List<Subscription> activeSubscriptions = subscriptionRepository.findAll().stream()
            .filter(s -> s.getPlan().getId().equals(id) && s.getStatus() == Subscription.Status.ACTIVE)
            .collect(java.util.stream.Collectors.toList());

        if (!activeSubscriptions.isEmpty()) {
            return ResponseEntity.badRequest()
                .body("No se puede eliminar el plan. Hay " + activeSubscriptions.size() + 
                      " suscripciones activas asociadas a este plan.");
        }

        // Eliminar el plan
        membershipPlanRepository.delete(plan);
        
        return ResponseEntity.ok("Plan de membresía eliminado correctamente");
    }
}
