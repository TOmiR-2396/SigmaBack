package com.example.gym.controller;

import com.example.gym.model.User;
import com.example.gym.repository.UserRepository;
import com.example.gym.service.RoleService;
import com.example.gym.service.PasswordResetService;
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
    private PasswordResetService passwordResetService;

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
        user.setStatus(User.UserStatus.INACTIVE); // Usuario inicia desactivado
        User savedUser = userRepository.save(user);
        roleService.assignDefaultRole(savedUser);
        // No exponer la contraseña ni datos sensibles
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());
        response.put("status", savedUser.getStatus());
        response.put("message", "Usuario registrado. Esperando activación por parte del administrador.");
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

    // ================= Recuperación de Contraseña =================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email es requerido");
            }
            
            // Verificar si el usuario existe
            if (!userRepository.existsByEmail(email)) {
                // Por seguridad, no revelar si el email existe o no
                return ResponseEntity.ok("Si el email existe, se enviará un enlace de recuperación");
            }
            
            String token = passwordResetService.createPasswordResetToken(email);
            
            // Aquí deberías enviar el email con el token
            // Por ahora, lo devolvemos en la respuesta (solo para desarrollo/testing)
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Token de recuperación generado");
            response.put("token", token); // EN PRODUCCIÓN: NO incluir esto, solo enviar por email
            response.put("resetUrl", "http://localhost:3000/reset-password?token=" + token);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al procesar solicitud: " + e.getMessage());
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Token es requerido");
            }
            
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body("La contraseña debe tener al menos 6 caracteres");
            }
            
            boolean success = passwordResetService.changePasswordWithToken(token, newPassword);
            
            if (success) {
                return ResponseEntity.ok("Contraseña cambiada exitosamente");
            } else {
                return ResponseEntity.badRequest().body("Token inválido o expirado");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al cambiar contraseña: " + e.getMessage());
        }
    }
    
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            boolean isValid = passwordResetService.validatePasswordResetToken(token);
            
            if (isValid) {
                // Obtener información del usuario para mostrar en el formulario
                Optional<User> userOpt = passwordResetService.getUserByToken(token);
                if (userOpt.isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("email", userOpt.get().getEmail());
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.ok(Map.of("valid", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al validar token: " + e.getMessage());
        }
    }

        // ================= Asignar TRAINER =================
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/assign-trainer")
    public ResponseEntity<?> assignTrainer(@RequestBody Map<String, Long> ids, Authentication authentication) {
        Long userId = ids.get("userId");
        User owner = (User) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.UserRole.TRAINER);
        user.setStatus(User.UserStatus.ACTIVE); // Los TRAINER siempre están activos
        userRepository.save(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Role changed to TRAINER by OWNER: " + owner.getEmail());
        response.put("userId", user.getId());
        response.put("userEmail", user.getEmail());
        response.put("newRole", user.getRole());
        response.put("status", user.getStatus());
        response.put("note", "Los usuarios TRAINER se activan automáticamente");
        
        return ResponseEntity.ok(response);
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

    // ================= Activar/Desactivar Usuarios =================
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/activate-user")
    public ResponseEntity<?> activateUser(@RequestBody Map<String, Long> ids, Authentication authentication) {
        Long userId = ids.get("userId");
        User owner = (User) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        return ResponseEntity.ok("Usuario activado por OWNER: " + owner.getEmail());
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/deactivate-user")
    public ResponseEntity<?> deactivateUser(@RequestBody Map<String, Long> ids, Authentication authentication) {
        Long userId = ids.get("userId");
        User owner = (User) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
        return ResponseEntity.ok("Usuario desactivado por OWNER: " + owner.getEmail());
    }

    // ================= Obtener mis datos =================
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("id", currentUser.getId());
            userProfile.put("email", currentUser.getEmail());
            userProfile.put("firstName", currentUser.getFirstName());
            userProfile.put("lastName", currentUser.getLastName());
            userProfile.put("phone", currentUser.getPhone());
            userProfile.put("birthDate", currentUser.getBirthDate());
            userProfile.put("joinDate", currentUser.getJoinDate());
            userProfile.put("role", currentUser.getRole());
            userProfile.put("status", currentUser.getStatus());
            
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al obtener datos del usuario: " + e.getMessage());
        }
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
}
