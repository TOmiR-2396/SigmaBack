package com.example.gym.controller;

import com.example.gym.model.User;
import com.example.gym.tenant.TenantContext;
import com.example.gym.tenant.TenantSwitchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Transactional
class UserInactivityPolicyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantSwitchRepository tenantSwitchRepository;

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
    void ownerCanGetAndUpdateInactivityPolicy() throws Exception {
        User owner = User.builder()
            .firstName("Owner")
            .lastName("One")
            .email("owner@test.com")
            .password("secret")
            .role(User.UserRole.OWNER)
            .status(User.UserStatus.ACTIVE)
            .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            owner,
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/users/inactivity-policy")
                .principal(auth)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days").exists())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/users/inactivity-policy")
                .principal(auth)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":21,\"enabled\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days").value(21))
            .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/api/users/inactivity-policy")
                .principal(auth)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days").value(21))
            .andExpect(jsonPath("$.enabled").value(true));
    }
}
