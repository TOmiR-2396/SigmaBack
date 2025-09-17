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
    @PreAuthorize("hasAnyRole('OWNER','TRAINER')")
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
    @PreAuthorize("hasAnyRole('OWNER','TRAINER')")
    @GetMapping("/user/{userId}/membership-status")
    public ResponseEntity<?> checkUserMembershipStatus(@PathVariable Long userId) {
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
                response.put("planName", subscription.getPlan().getName());
                response.put("startDate", subscription.getStartDate());
                response.put("endDate", endDate);
                response.put("daysLeft", daysLeft);
            }
        } else {
            response.put("membershipStatus", "NO_MEMBERSHIP");
            response.put("message", "Usuario sin membresía activa");
        }

        return ResponseEntity.ok(response);
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
