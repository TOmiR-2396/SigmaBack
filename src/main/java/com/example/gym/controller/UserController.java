package com.example.gym.controller;

import com.example.gym.model.User;
import com.example.gym.repository.UserRepository;
import com.example.gym.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.gym.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

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
    @PostMapping("/assign-trainer")
    public ResponseEntity<?> assignTrainer(@RequestBody Map<String, Long> ids) {
        Long userId = ids.get("userId");
        // Busca el único usuario OWNER en la base
        User owner = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst()
            .orElse(null);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No OWNER user found in the system");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        roleService.assignTrainer(user);
        return ResponseEntity.ok("Role changed to TRAINER by OWNER: " + owner.getEmail());
    }
    @PostMapping("/assign-member")
    public ResponseEntity<?> assignMember(@RequestBody Map<String, Long> ids) {
    Long userId = ids.get("userId");
    User user = userRepository.findById(userId).orElseThrow();
    user.setRole(User.UserRole.MEMBER);
    userRepository.save(user);
    return ResponseEntity.ok("Role changed to MEMBER");
}
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", u.getId());
            userMap.put("email", u.getEmail());
            userMap.put("role", u.getRole());
            userMap.put("firstName", u.getFirstName());
            userMap.put("lastName", u.getLastName());
            userMap.put("status", u.getStatus());
            result.add(userMap);
        }
        return ResponseEntity.ok(result);
    }
}
