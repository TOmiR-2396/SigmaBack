package com.example.gym.controller;

import com.example.gym.model.User;
import com.example.gym.repository.UserRepository;
import com.example.gym.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Transactional
class DeactivateNonMemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUpTenant() {
        TenantContext.setCurrentTenant("t1");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCannotDeactivateNonMember() throws Exception {
        User owner = userRepository.save(User.builder()
            .firstName("Owner")
            .lastName("Two")
            .email("owner3@test.com")
            .password("secret")
            .role(User.UserRole.OWNER)
            .status(User.UserStatus.ACTIVE)
            .build());

        User trainer = userRepository.save(User.builder()
            .firstName("Trainer")
            .lastName("Two")
            .email("trainer2@test.com")
            .password("secret")
            .role(User.UserRole.TRAINER)
            .status(User.UserStatus.ACTIVE)
            .build());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            owner,
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/api/users/{id}/deactivate", trainer.getId())
                .principal(auth)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"test\"}"))
            .andExpect(status().isForbidden());
    }
}
