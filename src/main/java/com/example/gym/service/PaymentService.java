package com.example.gym.service;

import com.example.gym.model.MembershipPlan;
import com.example.gym.model.PaymentRecord;
import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import com.example.gym.repository.MembershipPlanRepository;
import com.example.gym.repository.PaymentRecordRepository;
import com.example.gym.repository.SubscriptionRepository;
import com.example.gym.repository.UserRepository;
import com.example.gym.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final UserRepository userRepository;
    private final MembershipPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Activa la suscripción del usuario tras un pago aprobado por MP.
     *
     * @param externalReference formato: "tenantId:userId:planId"
     * @param mpPaymentId       ID del pago en MP
     */
    @Transactional
    public void activateSubscription(String externalReference, String mpPaymentId) {
        if (externalReference == null || externalReference.isBlank()) {
            log.warn("[Payment] Pago {} sin external_reference — no se activa ninguna suscripción.", mpPaymentId);
            return;
        }

        String[] parts = externalReference.split(":", 3);
        if (parts.length != 3) {
            log.warn("[Payment] external_reference inválido '{}' (pago {}). Formato esperado: tenantId:userId:planId",
                    externalReference, mpPaymentId);
            return;
        }

        String tenantId = parts[0];
        long userId, planId;
        try {
            userId = Long.parseLong(parts[1]);
            planId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            log.warn("[Payment] IDs no numéricos en external_reference '{}': {}", externalReference, e.getMessage());
            return;
        }

        TenantContext.setCurrentTenant(tenantId);
        enableTenantFilter(tenantId);
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("[Payment] Usuario {} no encontrado (pago {})", userId, mpPaymentId);
                return;
            }

            MembershipPlan plan = planRepository.findById(planId).orElse(null);
            if (plan == null) {
                log.warn("[Payment] Plan {} no encontrado (pago {})", planId, mpPaymentId);
                return;
            }

            // Cancelar suscripciones activas anteriores
            cancelActiveSubscriptions(user);

            // Crear la nueva suscripción
            LocalDate today = LocalDate.now();
            Subscription sub = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .startDate(today)
                    .endDate(today.plusMonths(plan.getDurationMonths()))
                    .status(Subscription.Status.ACTIVE)
                    .build();
            subscriptionRepository.save(sub);

            // Guardar historial de pago
            PaymentRecord record = PaymentRecord.builder()
                    .user(user)
                    .plan(plan)
                    .amount(plan.getPrice())
                    .method(PaymentRecord.PaymentMethod.MP)
                    .status(PaymentRecord.PaymentStatus.APPROVED)
                    .mpPaymentId(mpPaymentId)
                    .build();
            paymentRecordRepository.save(record);

            log.info("[Payment] Suscripción activada — usuario={} plan='{}' hasta={} (pago {})",
                    userId, plan.getName(), sub.getEndDate(), mpPaymentId);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Registra un pago en efectivo y activa la suscripción.
     *
     * @param userId        ID del usuario
     * @param planId        ID del plan
     * @param registeredBy  Usuario que registra el pago (owner/trainer)
     * @return La suscripción creada
     */
    @Transactional
    public Subscription registerCashPayment(Long userId, Long planId, User registeredBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + planId));

        // Cancelar suscripciones activas anteriores
        cancelActiveSubscriptions(user);

        // Crear nueva suscripción
        LocalDate today = LocalDate.now();
        Subscription sub = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(today)
                .endDate(today.plusMonths(plan.getDurationMonths()))
                .status(Subscription.Status.ACTIVE)
                .build();
        subscriptionRepository.save(sub);

        // Guardar historial de pago en efectivo
        PaymentRecord record = PaymentRecord.builder()
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .method(PaymentRecord.PaymentMethod.CASH)
                .status(PaymentRecord.PaymentStatus.APPROVED)
                .registeredBy(registeredBy)
                .build();
        paymentRecordRepository.save(record);

        log.info("[Payment] Pago en efectivo registrado — usuario={} plan='{}' registradoPor={}",
                userId, plan.getName(), registeredBy != null ? registeredBy.getId() : "system");

        return sub;
    }

    /**
     * Cancela todas las suscripciones activas de un usuario.
     */
    public void cancelActiveSubscriptions(User user) {
        List<Subscription> existing = subscriptionRepository.findByUser(user);
        existing.stream()
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
                .forEach(s -> {
                    s.setStatus(Subscription.Status.CANCELED);
                    subscriptionRepository.save(s);
                });
    }

    private void enableTenantFilter(String tenantId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") != null) {
                session.disableFilter("tenantFilter");
            }
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } catch (Exception e) {
            log.warn("[Payment] No se pudo habilitar filtro de tenant '{}': {}", tenantId, e.getMessage());
        }
    }
}
