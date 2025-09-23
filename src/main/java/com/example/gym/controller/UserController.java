package com.example.gym.controller;

import com.example.gym.model.User;
import com.example.gym.model.MembershipPlan;
import com.example.gym.model.Subscription;
import com.example.gym.repository.UserRepository;
import com.example.gym.repository.MembershipPlanRepository;
import com.example.gym.repository.SubscriptionRepository;
import com.example.gym.service.RoleService;
import com.example.gym.service.MembershipService;
import com.example.gym.dto.MembershipInfoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.example.gym.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/auth","/api"})
public class UserController {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private WebClient webClient;

    public UserController(UserRepository userRepository, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= Register =================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        roleService.assignDefaultRole(savedUser);
        // No exponer la contraseña ni datos sensibles
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());
        //        response.put("status", savedUser.getStatus());
        return ResponseEntity.ok(response);
    }

    // ================= Login =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(request.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.get("password"), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("email", user.getEmail());
        response.put("id", user.getId());
        return ResponseEntity.ok(response);
    }

    // ================= Asignar TRAINER =================
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/assign-trainer")
    public ResponseEntity<?> assignTrainer(@RequestBody Map<String, Long> ids, Authentication authentication) {
        Long userId = ids.get("userId");
        User owner = (User) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        roleService.assignTrainer(user);
        return ResponseEntity.ok("Role changed to TRAINER by OWNER: " + owner.getEmail());
    }
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/assign-member")
    public ResponseEntity<?> assignMember(@RequestBody Map<String, Long> ids, Authentication authentication) {
        Long userId = ids.get("userId");
        User owner = (User) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.UserRole.MEMBER);
        userRepository.save(user);
        return ResponseEntity.ok("Role changed to MEMBER by OWNER: " + owner.getEmail());
    }
    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(mapUsers(userRepository.findAll()));
    }

    @PreAuthorize("hasAnyRole('OWNER','TRAINER')")
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam(name = "q", required = false) String q) {
        List<User> users = userRepository.searchUsers(q == null || q.isBlank() ? null : q.trim());
        return ResponseEntity.ok(mapUsers(users));
    }

    private List<Map<String,Object>> mapUsers(List<User> users) {
        List<Map<String,Object>> result = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String,Object> userMap = new HashMap<>();
            userMap.put("id", u.getId());
            userMap.put("email", u.getEmail());
            userMap.put("role", u.getRole());
            userMap.put("firstName", u.getFirstName());
            userMap.put("lastName", u.getLastName());
            userMap.put("status", u.getStatus());
            result.add(userMap);
        }
        return result;
    }

    // ================= Crear Suscripción con Verxor =================
    @PostMapping("/create-verxor-subscription")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<?> createVerxorSubscription(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            // Obtener usuario autenticado
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Obtener planId del request
            Long planId = Long.valueOf(request.get("planId").toString());
            
            // Buscar el plan de membresía
            Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(planId);
            if (planOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Plan de membresía no encontrado");
            }
            
            MembershipPlan plan = planOpt.get();
            
            // Verificar que el usuario no tenga una suscripción activa
            Optional<Subscription> existingSubscription = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) && s.getStatus() == Subscription.Status.ACTIVE)
                .findFirst();
            
            if (existingSubscription.isPresent()) {
                return ResponseEntity.badRequest().body("Ya tienes una membresía activa");
            }
            
            // Crear el cuerpo de la suscripción para Verxor
            Map<String, Object> subscriptionBody = new HashMap<>();
            subscriptionBody.put("name", plan.getName());
            subscriptionBody.put("description", "Membresía de gimnasio - " + plan.getName());
            subscriptionBody.put("interval", "month");
            subscriptionBody.put("price", plan.getPrice());
            subscriptionBody.put("currency", "ARS"); // O la moneda que uses
            subscriptionBody.put("successRedirect", "http://localhost:3000/membership-success");
            subscriptionBody.put("cancelRedirect", "http://localhost:3000/membership-cancel");
            
            // Información del cliente
            Map<String, Object> customer = new HashMap<>();
            customer.put("email", user.getEmail());
            customer.put("name", user.getFirstName() + " " + user.getLastName());
            subscriptionBody.put("customer", customer);
            
            // Metadata para identificar el plan y usuario
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", user.getId().toString());
            metadata.put("planId", plan.getId().toString());
            metadata.put("source", "gym_membership");
            subscriptionBody.put("metadata", metadata);
            
            // Llamar a Verxor para crear la suscripción
            try {
                String verxorResponse = webClient
                    .post()
                    .uri("/subscribe/mercadopago")
                    .header("Content-Type", "application/json")
                    .bodyValue(subscriptionBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                // Parsear la respuesta para obtener la URL de pago
                // Nota: Aquí deberías usar un parser JSON real como ObjectMapper
                // Por simplicidad, asumo que la respuesta contiene payment_url
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Suscripción creada exitosamente");
                response.put("planName", plan.getName());
                response.put("price", plan.getPrice());
                response.put("verxorResponse", verxorResponse);
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error al crear suscripción en Verxor: " + e.getMessage());
            }
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ID del plan debe ser un número válido");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    // ================= Obtener Planes de Membresía para Suscripción =================
    @GetMapping("/membership-plans")
    public ResponseEntity<?> getMembershipPlans() {
        try {
            List<MembershipPlan> plans = membershipPlanRepository.findAll();
            
            List<Map<String, Object>> planDTOs = plans.stream().map(plan -> {
                Map<String, Object> planMap = new HashMap<>();
                planMap.put("id", plan.getId());
                planMap.put("name", plan.getName());
                planMap.put("price", plan.getPrice());
                planMap.put("durationMonths", plan.getDurationMonths());
                planMap.put("daysPerWeek", plan.getDaysPerWeek());
                
                // Información formateada para mostrar al usuario
                planMap.put("description", String.format("Plan %s - %d días por semana durante %d meses", 
                    plan.getName(), plan.getDaysPerWeek(), plan.getDurationMonths()));
                planMap.put("interval", "month"); // Para compatibilidad con Verxor
                planMap.put("currency", "ARS"); // Ajustar según tu moneda
                
                return planMap;
            }).toList();
            
            return ResponseEntity.ok(planDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener planes: " + e.getMessage());
        }
    }

    // ================= Webhook Verxor - Activación automática por pago =================
    @PostMapping("/verxor-subscription-webhook")
    public ResponseEntity<?> handleVerxorSubscriptionWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            // Log para debug
            System.out.println("Webhook recibido de Verxor: " + webhookData);
            
            // Extraer información del webhook
            String status = (String) webhookData.get("status");
            Map<String, Object> metadata = (Map<String, Object>) webhookData.get("metadata");
            
            if (metadata == null) {
                return ResponseEntity.badRequest().body("Metadata no encontrada en webhook");
            }
            
            String userIdStr = (String) metadata.get("userId");
            String planIdStr = (String) metadata.get("planId");
            String source = (String) metadata.get("source");
            
            if (userIdStr == null || planIdStr == null || !"gym_membership".equals(source)) {
                return ResponseEntity.badRequest().body("Metadata inválida en webhook");
            }
            
            // Solo procesar si el pago fue exitoso
            if (!"completed".equalsIgnoreCase(status) && !"active".equalsIgnoreCase(status)) {
                System.out.println("Webhook ignorado - estado: " + status);
                return ResponseEntity.ok("Webhook procesado - estado no activo");
            }
            
            Long userId = Long.valueOf(userIdStr);
            Long planId = Long.valueOf(planIdStr);
            
            // Buscar usuario y plan
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(planId);
            
            if (userOpt.isEmpty() || planOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Usuario o plan no encontrado");
            }
            
            User user = userOpt.get();
            MembershipPlan plan = planOpt.get();
            
            // Verificar si ya tiene una suscripción activa para evitar duplicados
            Optional<Subscription> existingSubscription = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
                .findFirst();
            
            if (existingSubscription.isPresent()) {
                System.out.println("Usuario ya tiene suscripción activa - webhook ignorado");
                return ResponseEntity.ok("Usuario ya tiene suscripción activa");
            }
            
            // Activar usuario automáticamente
            user.setStatus(User.UserStatus.ACTIVE);
            userRepository.save(user);
            
            // Crear nueva suscripción
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(plan);
            subscription.setStartDate(java.time.LocalDate.now());
            
            // Calcular fecha de vencimiento
            Integer duration = plan.getDurationMonths();
            if (duration == null || duration < 1) {
                duration = 1;
            }
            subscription.setEndDate(java.time.LocalDate.now().plusMonths(duration));
            subscription.setStatus(Subscription.Status.ACTIVE);
            
            subscriptionRepository.save(subscription);
            
            System.out.println("Suscripción creada automáticamente para usuario: " + user.getEmail());
            
            return ResponseEntity.ok("Suscripción activada exitosamente");
            
        } catch (Exception e) {
            System.err.println("Error procesando webhook de Verxor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error procesando webhook: " + e.getMessage());
        }
    }

    // ================= Activar Usuario y Asignar Membresía =================
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/activate-user-membership")
    public ResponseEntity<?> activateUserAndAssignMembership(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            // Obtener parámetros
            Long userId = Long.valueOf(request.get("userId").toString());
            Long planId = Long.valueOf(request.get("planId").toString());
            
            // Validar que el usuario autenticado sea OWNER
            Optional<User> ownerOpt = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.OWNER)
                .findFirst();
            if (ownerOpt.isEmpty()) {
                return ResponseEntity.status(403).body("Solo OWNER puede activar usuarios y asignar membresías");
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

            User user = userOpt.get();
            MembershipPlan plan = planOpt.get();

            // Validar que el usuario destino sea MEMBER
            if (user.getRole() != User.UserRole.MEMBER) {
                return ResponseEntity.badRequest().body("Solo se puede activar y asignar membresía a usuarios con rol MEMBER");
            }

            // Activar usuario (cambiar status a ACTIVE)
            user.setStatus(User.UserStatus.ACTIVE);
            userRepository.save(user);

            // Verificar si ya tiene una suscripción activa
            Optional<Subscription> existingSubscription = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
                .findFirst();

            if (existingSubscription.isPresent()) {
                return ResponseEntity.badRequest().body("El usuario ya tiene una membresía activa");
            }

            // Crear nueva suscripción
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(plan);
            subscription.setStartDate(java.time.LocalDate.now());
            
            // Calcular fecha de vencimiento
            Integer duration = plan.getDurationMonths();
            if (duration == null || duration < 1) {
                duration = 1;
            }
            subscription.setEndDate(java.time.LocalDate.now().plusMonths(duration));
            subscription.setStatus(Subscription.Status.ACTIVE);
            
            subscriptionRepository.save(subscription);

            // Crear respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Usuario activado y membresía asignada correctamente");
            response.put("userId", user.getId());
            response.put("userEmail", user.getEmail());
            response.put("userName", user.getFirstName() + " " + user.getLastName());
            response.put("userStatus", user.getStatus());
            response.put("planName", plan.getName());
            response.put("startDate", subscription.getStartDate());
            response.put("endDate", subscription.getEndDate());
            response.put("durationMonths", plan.getDurationMonths());
            response.put("price", plan.getPrice());
            response.put("daysPerWeek", plan.getDaysPerWeek());

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("IDs deben ser números válidos");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    // ================= Verificar Estado de Membresía =================
    @GetMapping("/user/{userId}/membership-status")
    public ResponseEntity<?> checkUserMembershipStatus(@PathVariable Long userId, Authentication authentication) {
        try {
            // Si no hay autenticación, retornar error
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de autenticación requerido");
            }
            
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userRepository.findByEmail(currentUserEmail);
            
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            }
            
            User currentUser = currentUserOpt.get();
            
            // Verificar permisos: 
            // - OWNER y TRAINER pueden ver cualquier usuario
            // - MEMBER solo puede ver su propia información
            boolean isOwnerOrTrainer = currentUser.getRole() == User.UserRole.OWNER || 
                                      currentUser.getRole() == User.UserRole.TRAINER;
            
            boolean isSelfRequest = currentUser.getId().equals(userId);
            
            if (!isOwnerOrTrainer && !isSelfRequest) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
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

            Map<String, Object> response = new HashMap<>();
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
                    Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(subscription.getPlan().getId());
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al verificar estado de membresía: " + e.getMessage());
        }
    }

    // ================= Mi Estado de Membresía (para usuario autenticado) =================
    @GetMapping("/my-membership-status")
    public ResponseEntity<?> getMyMembershipStatus(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de autenticación requerido");
            }
            
            String userEmail = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            }
            
            User user = userOpt.get();
            
            // Reutilizar la lógica del endpoint anterior
            return checkUserMembershipStatus(user.getId(), authentication);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al obtener estado de membresía: " + e.getMessage());
        }
    }

    // ================= Cambiar Estado de Usuario (Solo activar si ya tiene membresía) =================
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            
            // Obtener el nuevo estado del request body
            boolean isActive = Boolean.parseBoolean(request.get("active").toString());
            
            // Cambiar el estado
            if (isActive) {
                // Para activar un usuario, debe tener al menos una suscripción (activa, cancelada o expirada)
                boolean hasAnySubscription = subscriptionRepository.findAll().stream()
                    .anyMatch(s -> s.getUser().getId().equals(userId));
                
                if (!hasAnySubscription) {
                    return ResponseEntity.badRequest().body(
                        "No se puede activar usuario sin membresía. Use 'activate-user-membership' para asignar una membresía primero."
                    );
                }
                
                user.setStatus(User.UserStatus.ACTIVE);
                
                // Si el usuario se reactiva, reactivar también su suscripción más reciente si estaba cancelada
                Optional<Subscription> lastCanceledSubscription = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.CANCELED)
                    .max((s1, s2) -> s1.getStartDate().compareTo(s2.getStartDate()));
                
                if (lastCanceledSubscription.isPresent()) {
                    Subscription subscription = lastCanceledSubscription.get();
                    // Solo reactivar si no ha expirado
                    if (subscription.getEndDate().isAfter(java.time.LocalDate.now())) {
                        subscription.setStatus(Subscription.Status.ACTIVE);
                        subscriptionRepository.save(subscription);
                    }
                }
                
            } else {
                user.setStatus(User.UserStatus.INACTIVE);
                
                // Si se desactiva el usuario, cancelar sus suscripciones activas
                List<Subscription> activeSubscriptions = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
                    .toList();
                
                for (Subscription subscription : activeSubscriptions) {
                    subscription.setStatus(Subscription.Status.CANCELED);
                    subscriptionRepository.save(subscription);
                }
            }
            
            userRepository.save(user);

            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("status", user.getStatus());
            response.put("message", isActive ? 
                "Usuario reactivado correctamente" : 
                "Usuario desactivado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al actualizar estado: " + e.getMessage());
        }
    }

    // ================= Membership Info Enhanced =================
    @GetMapping("/membership-info-enhanced")
    @PreAuthorize("hasRole('MEMBER') or hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<MembershipInfoDTO> getMembershipInfoEnhanced(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            MembershipInfoDTO membershipInfo = membershipService.getMembershipInfo(userEmail);
            return ResponseEntity.ok(membershipInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
