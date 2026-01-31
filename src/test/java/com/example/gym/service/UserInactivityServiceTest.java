package com.example.gym.service;

import com.example.gym.model.User;
import com.example.gym.repository.UserRepository;
import com.example.gym.tenant.TenantContext;
import com.example.gym.tenant.TenantSwitch;
import com.example.gym.tenant.TenantSwitchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserInactivityServiceTest {

    @Autowired
    private UserInactivityService userInactivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantSwitchRepository tenantSwitchRepository;

    @BeforeEach
    void setUpTenant() {
        TenantContext.setCurrentTenant("t1");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void schedulerDeactivatesInactiveMember() {
        User member = userRepository.save(User.builder()
            .firstName("Member")
            .lastName("Inactive")
            .email("inactive@test.com")
            .password("secret")
            .role(User.UserRole.MEMBER)
            .status(User.UserStatus.ACTIVE)
            .lastLoginAt(LocalDateTime.now().minusDays(30))
            .build());

        tenantSwitchRepository.save(TenantSwitch.builder()
            .key("INACTIVITY_AUTO_DEACTIVATE")
            .enabled(true)
            .payload("14")
            .build());

        userInactivityService.deactivateInactiveUsers();

        User updated = userRepository.findById(member.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
        assertThat(updated.getDeactivationReason()).isEqualTo("INACTIVITY");
        assertThat(updated.getDeactivatedByRole()).isEqualTo("SYSTEM");
    }
}
