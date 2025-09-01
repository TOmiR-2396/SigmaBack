package com.example.gym.service;

import com.example.gym.model.Member;
import com.example.gym.model.Role;
import com.example.gym.model.Role.RoleName;
import com.example.gym.repository.MemberRepository;
import com.example.gym.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class RoleService {

    // Repositorios marcados como final para que Lombok genere el constructor y Spring los inyecte
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    
    // Asigna MEMBER por defecto a un usuario nuevo
    public void assignDefaultRole(Member member) {
        // Busca el rol MEMBER en la base
        Role memberRole = roleRepository.findByName(RoleName.MEMBER)
                .orElseThrow(() -> new RuntimeException("ROLE_MEMBER not found"));

        // Inicializa el set de roles si es null
        if (member.getRoles() == null) {
            member.setRoles(new HashSet<>());
        }

        member.getRoles().add(memberRole);
        memberRepository.save(member);
    }

    // OWNER asigna TRAINER a otro usuario
    public void assignTrainer(Member owner, Member trainerCandidate) {
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
        memberRepository.save(trainerCandidate);
    }
}
