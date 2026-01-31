package com.example.gym.controller;

import com.example.gym.model.Reservation;
import com.example.gym.model.Schedule;
import com.example.gym.model.User;
import com.example.gym.repository.ReservationRepository;
import com.example.gym.repository.ScheduleRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Transactional
class ReservationCancellationOwnerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

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
    void ownerCancelsReservation_setsAuditFields() throws Exception {
        User owner = userRepository.save(User.builder()
            .firstName("Owner")
            .lastName("One")
            .email("owner2@test.com")
            .password("secret")
            .role(User.UserRole.OWNER)
            .status(User.UserStatus.ACTIVE)
            .build());

        User member = userRepository.save(User.builder()
            .firstName("Member")
            .lastName("Two")
            .email("member2@test.com")
            .password("secret")
            .role(User.UserRole.MEMBER)
            .status(User.UserStatus.ACTIVE)
            .build());

        Schedule schedule = scheduleRepository.save(Schedule.builder()
            .dayOfWeek(2)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .maxCapacity(10)
            .isActive(true)
            .repeatWeekly(true)
            .build());

        Reservation reservation = reservationRepository.save(Reservation.builder()
            .user(member)
            .schedule(schedule)
            .date(LocalDate.now().plusDays(2))
            .status(Reservation.ReservationStatus.CONFIRMED)
            .build());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            owner,
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/api/turnos/reservation/{id}", reservation.getId())
                .principal(auth)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);
        assertThat(updated.getCancelledByUserId()).isEqualTo(owner.getId());
        assertThat(updated.getCancelledByRole()).isEqualTo("OWNER");
        assertThat(updated.getCancellationReason()).isEqualTo("REMOVED_BY_STAFF");
    }
}
