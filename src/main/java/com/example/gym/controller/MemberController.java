package com.example.gym.controller;

import com.example.gym.model.Member;
import com.example.gym.repository.MemberRepository;
import com.example.gym.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class MemberController {

    private final MemberRepository memberRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public MemberController(MemberRepository memberRepository, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= Register =================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        // Validar email duplicado
        if (memberRepository.existsByEmail(member.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // Encriptar la contraseña
        member.setPassword(passwordEncoder.encode(member.getPassword()));

        // Guardar el miembro
        Member savedMember = memberRepository.save(member);

        // Asignar rol MEMBER por defecto
        roleService.assignDefaultRole(savedMember);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedMember);
    }

    // ================= Login =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        Member member = memberRepository.findByEmail(request.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.get("password"), member.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }

        return ResponseEntity.ok(member);
    }
}
