package com.example.gym.service;

import com.example.gym.model.User;
import com.example.gym.model.Role;
import com.example.gym.model.Role.RoleName;
import com.example.gym.repository.UserRepository;
import com.example.gym.repository.RoleRepository;
// import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // Constructor manual para inyección de dependencias
    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }
    
    // Asigna MEMBER por defecto a un usuario nuevo
    public void assignDefaultRole(User user) {
        // Busca el rol MEMBER en la base
        Role memberRole = roleRepository.findByName(RoleName.MEMBER)
                .orElseThrow(() -> new RuntimeException("ROLE_MEMBER not found"));

        // Inicializa el set de roles si es null
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        user.getRoles().add(memberRole);
        userRepository.save(user);
    }

    // OWNER asigna TRAINER a otro usuario
    public void assignTrainer(User owner, User trainerCandidate) {
        // Verifica que el owner tenga el rol OWNER
    if (owner.getRoles() == null || owner.getRoles().stream().noneMatch(r -> r.getName() == RoleName.OWNER)) {
            throw new RuntimeException("Only OWNER can assign TRAINERS");
        }

        // Busca el rol TRAINER en la base
        Role trainerRole = roleRepository.findByName(RoleName.TRAINER)
                .orElseThrow(() -> new RuntimeException("ROLE_TRAINER not found"));

        // Inicializa el set de roles del candidato si es null
        if (trainerCandidate.getRoles() == null) {
            trainerCandidate.setRoles(new HashSet<>());
        }

        trainerCandidate.getRoles().add(trainerRole);
        userRepository.save(trainerCandidate);
    }
}
