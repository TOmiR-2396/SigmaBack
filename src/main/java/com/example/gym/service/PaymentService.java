package com.example.gym.service;

import com.example.gym.dto.*;
import com.example.gym.model.*;
import com.example.gym.repository.*;
import com.example.gym.config.VerxorConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebClient verxorWebClient;
    private final VerxorConfiguration verxorConfiguration;

    @Value("${verxor.webhook.url}")
    private String webhookUrl;

    @Value("${verxor.success.url}")
    private String successUrl;

    @Value("${verxor.failure.url}")
    private String failureUrl;

    @Value("${verxor.pending.url}")
    private String pendingUrl;

    @Transactional
    public PaymentResponseDTO createPaymentPreference(PaymentRequestDTO request, String userEmail) {
        try {
            // Buscar usuario
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Buscar plan de membresía
            MembershipPlan membershipPlan = membershipPlanRepository.findById(request.getMembershipPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan de membresía no encontrado"));

            // Generar referencia externa única
            String externalReference = UUID.randomUUID().toString();

            // Crear payment record en BD
            Payment payment = Payment.builder()
                    .user(user)
                    .membershipPlan(membershipPlan)
                    .amount(BigDecimal.valueOf(membershipPlan.getPrice()))
                    .status(Payment.PaymentStatus.PENDING)
                    .externalReference(externalReference)
                    .description(request.getDescription() != null ? 
                            request.getDescription() : 
                            "Membresía " + membershipPlan.getName())
                    .payerEmail(request.getPayerEmail() != null ? 
                            request.getPayerEmail() : 
                            user.getEmail())
                    .currencyId("CLP")
                    .notificationUrl(webhookUrl)
                    .build();

            payment = paymentRepository.save(payment);

            // Crear payload para Verxor
            Map<String, Object> verxorPayload = new HashMap<>();
            verxorPayload.put("project_id", verxorConfiguration.getProjectId());
            verxorPayload.put("amount", membershipPlan.getPrice().intValue());
            verxorPayload.put("currency", "CLP");
            verxorPayload.put("description", "Membresía " + membershipPlan.getName());
            verxorPayload.put("external_reference", externalReference);
            verxorPayload.put("success_url", successUrl + "?external_reference=" + externalReference);
            verxorPayload.put("failure_url", failureUrl + "?external_reference=" + externalReference);
            verxorPayload.put("pending_url", pendingUrl + "?external_reference=" + externalReference);
            verxorPayload.put("webhook_url", webhookUrl);
            
            Map<String, Object> payer = new HashMap<>();
            payer.put("email", payment.getPayerEmail());
            payer.put("first_name", user.getFirstName());
            payer.put("last_name", user.getLastName());
            verxorPayload.put("payer", payer);

            // Llamar a la API de Verxor
            Map<String, Object> verxorResponse = verxorWebClient
                    .post()
                    .uri("/payments/create")
                    .bodyValue(verxorPayload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (verxorResponse != null && verxorResponse.containsKey("payment_url")) {
                String paymentUrl = (String) verxorResponse.get("payment_url");
                String paymentId = (String) verxorResponse.get("payment_id");
                
                // Actualizar payment con datos de Verxor
                payment.setPreferenceId(paymentId);
                paymentRepository.save(payment);

                log.info("Pago Verxor creado exitosamente. ID: {}, External Reference: {}", 
                        paymentId, externalReference);

                return PaymentResponseDTO.builder()
                        .preferenceId(paymentId)
                        .initPoint(paymentUrl)
                        .sandboxInitPoint(paymentUrl) // Verxor usa la misma URL
                        .externalReference(externalReference)
                        .message("Pago creado exitosamente")
                        .success(true)
                        .build();
            } else {
                throw new RuntimeException("Respuesta inválida de Verxor");
            }

        } catch (Exception e) {
            log.error("Error creando pago en Verxor: {}", e.getMessage());
            return PaymentResponseDTO.builder()
                    .message("Error creando pago: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    @Transactional
    public void processPaymentNotification(String paymentId) {
        try {
            // Consultar el estado del pago en Verxor
            Map<String, Object> verxorResponse = verxorWebClient
                    .get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (verxorResponse != null) {
                String externalReference = (String) verxorResponse.get("external_reference");
                String status = (String) verxorResponse.get("status");
                
                Optional<com.example.gym.model.Payment> paymentOpt = paymentRepository.findByExternalReference(externalReference);

                if (paymentOpt.isPresent()) {
                    com.example.gym.model.Payment payment = paymentOpt.get();
                    
                    // Actualizar información del pago
                    payment.setPaymentId(paymentId);
                    payment.setPaymentMethodId((String) verxorResponse.get("payment_method"));
                    payment.setPaymentTypeId((String) verxorResponse.get("payment_type"));
                    
                    Object amountObj = verxorResponse.get("amount");
                    if (amountObj instanceof Number) {
                        payment.setTransactionAmount(BigDecimal.valueOf(((Number) amountObj).doubleValue()));
                    }
                    
                    payment.setStatus(mapVerxorStatus(status));

                    if ("approved".equals(status) || "completed".equals(status)) {
                        payment.setApprovedAt(LocalDateTime.now());
                        
                        // Crear suscripción para el usuario
                        createSubscriptionFromPayment(payment);
                        
                        log.info("Pago aprobado y suscripción creada para usuario: {}", payment.getUser().getEmail());
                    } else if ("rejected".equals(status) || "failed".equals(status)) {
                        payment.setFailureMessage((String) verxorResponse.get("failure_reason"));
                    }

                    paymentRepository.save(payment);
                    log.info("Payment actualizado exitosamente. ID: {}, Status: {}", paymentId, status);
                } else {
                    log.warn("Payment no encontrado para external reference: {}", externalReference);
                }
            }

        } catch (Exception e) {
            log.error("Error procesando notificación de pago {}: {}", paymentId, e.getMessage());
        }
    }

    @Transactional
    private void createSubscriptionFromPayment(Payment payment) {
        try {
            User user = payment.getUser();
            MembershipPlan plan = payment.getMembershipPlan();

            // Verificar si ya existe una suscripción activa
            Optional<Subscription> existingSubscription = subscriptionRepository
                    .findByUserAndStatus(user, Subscription.Status.ACTIVE);

            if (existingSubscription.isPresent()) {
                // Si existe, extender la fecha de expiración
                Subscription subscription = existingSubscription.get();
                LocalDate newExpiryDate = subscription.getEndDate().plusMonths(plan.getDurationMonths());
                subscription.setEndDate(newExpiryDate);
                subscriptionRepository.save(subscription);
                log.info("Suscripción extendida hasta: {} para usuario: {}", newExpiryDate, user.getEmail());
            } else {
                // Crear nueva suscripción
                Subscription subscription = Subscription.builder()
                        .user(user)
                        .plan(plan)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusMonths(plan.getDurationMonths()))
                        .status(Subscription.Status.ACTIVE)
                        .build();

                subscriptionRepository.save(subscription);
                
                // Actualizar estado del usuario a MEMBER si no lo está
                if (user.getStatus() != User.UserStatus.ACTIVE) {
                    user.setStatus(User.UserStatus.ACTIVE);
                    userRepository.save(user);
                }

                log.info("Nueva suscripción creada para usuario: {} hasta: {}", 
                        user.getEmail(), subscription.getEndDate());
            }

        } catch (Exception e) {
            log.error("Error creando suscripción desde pago: {}", e.getMessage());
            throw new RuntimeException("Error creando suscripción", e);
        }
    }

    private Payment.PaymentStatus mapVerxorStatus(String verxorStatus) {
        return switch (verxorStatus.toLowerCase()) {
            case "approved", "completed", "paid" -> Payment.PaymentStatus.APPROVED;
            case "pending", "processing" -> Payment.PaymentStatus.PENDING;
            case "authorized" -> Payment.PaymentStatus.AUTHORIZED;
            case "in_process", "in_progress" -> Payment.PaymentStatus.IN_PROCESS;
            case "rejected", "failed", "declined" -> Payment.PaymentStatus.REJECTED;
            case "cancelled", "canceled" -> Payment.PaymentStatus.CANCELLED;
            case "refunded" -> Payment.PaymentStatus.REFUNDED;
            default -> Payment.PaymentStatus.PENDING;
        };
    }

    public List<PaymentDTO> getUserPayments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Payment> payments = paymentRepository.findByUserOrderByCreatedAtDesc(user);
        return payments.stream().map(this::convertToDTO).toList();
    }

    public List<PaymentDTO> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        return payments.stream().map(this::convertToDTO).toList();
    }

    public Optional<PaymentDTO> getPaymentByPreferenceId(String preferenceId) {
        return paymentRepository.findByPreferenceId(preferenceId)
                .map(this::convertToDTO);
    }

    public List<Payment> findExpiredPendingPayments() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return paymentRepository.findExpiredPendingPayments(oneHourAgo);
    }

    private PaymentDTO convertToDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .preferenceId(payment.getPreferenceId())
                .paymentId(payment.getPaymentId())
                .paymentMethodId(payment.getPaymentMethodId())
                .paymentTypeId(payment.getPaymentTypeId())
                .userId(payment.getUser().getId())
                .userName(payment.getUser().getFirstName() + " " + payment.getUser().getLastName())
                .membershipPlanId(payment.getMembershipPlan().getId())
                .membershipPlanName(payment.getMembershipPlan().getName())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .externalReference(payment.getExternalReference())
                .transactionAmount(payment.getTransactionAmount())
                .currencyId(payment.getCurrencyId())
                .description(payment.getDescription())
                .payerEmail(payment.getPayerEmail())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .approvedAt(payment.getApprovedAt())
                .failureMessage(payment.getFailureMessage())
                .build();
    }
}
