package com.example.gym.service;

import com.example.gym.model.User;
import com.example.gym.model.Role;
import com.example.gym.repository.UserRepository;
// import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class RoleService {
    private final UserRepository userRepository;

    public RoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Asigna MEMBER por defecto a un usuario nuevo
    public void assignDefaultRole(User user) {
    user.setRole(User.UserRole.MEMBER);
        userRepository.save(user);
    }

    // OWNER asigna TRAINER a otro usuario (la verificación de OWNER se hace en el controlador)
    public void assignTrainer(User trainerCandidate) {
        trainerCandidate.setRole(User.UserRole.TRAINER);
        userRepository.save(trainerCandidate);
    }
}
