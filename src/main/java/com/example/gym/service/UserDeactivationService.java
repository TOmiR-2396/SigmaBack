package com.example.gym.service;

import com.example.gym.model.Reservation;
import com.example.gym.model.User;
import com.example.gym.repository.ReservationRepository;
import com.example.gym.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserDeactivationService {

    private static final Logger logger = LoggerFactory.getLogger(UserDeactivationService.class);

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public UserDeactivationService(UserRepository userRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public void deactivateUser(User target, User actor, String reason) {
        if (target.getStatus() == User.UserStatus.INACTIVE) {
            return;
        }

        target.setStatus(User.UserStatus.INACTIVE);
        target.setDeactivatedAt(LocalDateTime.now());
        if (actor != null) {
            target.setDeactivatedByUserId(actor.getId());
            target.setDeactivatedByRole(actor.getRole().name());
        } else {
            target.setDeactivatedByUserId(null);
            target.setDeactivatedByRole("SYSTEM");
        }
        target.setDeactivationReason(reason != null && !reason.isBlank() ? reason : "MANUAL");
        userRepository.save(target);

        cancelFutureReservations(target);
    }

    @Transactional
    public int deactivateInactiveMembers(String tenantId, LocalDate cutoffDate, String reason) {
        LocalDateTime cutoffDateTime = cutoffDate.atStartOfDay();
        List<User> candidates = userRepository.findInactiveCandidates(tenantId, cutoffDateTime, cutoffDate);
        int deactivated = 0;
        for (User user : candidates) {
            if (user.getRole() != User.UserRole.MEMBER || user.getStatus() != User.UserStatus.ACTIVE) {
                continue;
            }
            user.setStatus(User.UserStatus.INACTIVE);
            user.setDeactivatedAt(LocalDateTime.now());
            user.setDeactivatedByUserId(null);
            user.setDeactivatedByRole("SYSTEM");
            user.setDeactivationReason(reason);
            userRepository.save(user);
            cancelFutureReservations(user);
            deactivated++;
        }
        return deactivated;
    }

    private void cancelFutureReservations(User user) {
        List<Reservation> futureReservations = reservationRepository.findFutureConfirmedByUser(user.getId(), LocalDate.now());
        if (futureReservations.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Reservation reservation : futureReservations) {
            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            reservation.setCancelledAt(now);
        }
        reservationRepository.saveAll(futureReservations);
        logger.info("Canceladas {} reservas futuras para el usuario {}", futureReservations.size(), user.getId());
    }
}
