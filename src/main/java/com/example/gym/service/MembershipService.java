package com.example.gym.service;

import com.example.gym.dto.MembershipInfoDTO;
import com.example.gym.model.*;
import com.example.gym.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public MembershipInfoDTO getMembershipInfo(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Optional<Subscription> activeSubscription = subscriptionRepository
                .findByUserAndStatus(user, Subscription.Status.ACTIVE);

        if (activeSubscription.isPresent()) {
            Subscription subscription = activeSubscription.get();
            
            // Verificar si la suscripción ha expirado
            if (subscription.getEndDate().isBefore(LocalDate.now())) {
                // Marcar como expirada
                subscription.setStatus(Subscription.Status.EXPIRED);
                subscriptionRepository.save(subscription);
                
                return createInactiveMembershipInfo(user);
            }

            // Contar pagos aprobados
            Long approvedPayments = paymentRepository.countApprovedPaymentsByUser(user);

            return MembershipInfoDTO.builder()
                    .active(true)
                    .membershipPlan(subscription.getPlan().getName())
                    .startDate(subscription.getStartDate())
                    .endDate(subscription.getEndDate())
                    .daysRemaining(calculateDaysRemaining(subscription.getEndDate()))
                    .totalPayments(approvedPayments.intValue())
                    .userStatus(user.getStatus().toString())
                    .membershipStatus(subscription.getStatus().toString())
                    .build();
        } else {
            return createInactiveMembershipInfo(user);
        }
    }

    private MembershipInfoDTO createInactiveMembershipInfo(User user) {
        Long approvedPayments = paymentRepository.countApprovedPaymentsByUser(user);
        
        return MembershipInfoDTO.builder()
                .active(false)
                .membershipPlan("Sin membresía activa")
                .startDate(null)
                .endDate(null)
                .daysRemaining(0)
                .totalPayments(approvedPayments.intValue())
                .userStatus(user.getStatus().toString())
                .membershipStatus("INACTIVE")
                .build();
    }

    private int calculateDaysRemaining(LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate.isBefore(today)) {
            return 0;
        }
        return (int) today.until(endDate).getDays();
    }

    @Transactional
    public void checkAndUpdateExpiredMemberships() {
        // Este método puede ser llamado por un scheduler para verificar membresías expiradas
        log.info("Verificando membresías expiradas...");
        
        // Implementar lógica para actualizar membresías expiradas si es necesario
        // Por ahora, las membresías se marcan como expiradas en getMembershipInfo
    }
}
