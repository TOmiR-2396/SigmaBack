package com.example.gym.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preference_id", nullable = false, unique = true)
    private String preferenceId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "payment_type_id")
    private String paymentTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_plan_id", nullable = false)
    private MembershipPlan membershipPlan;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "transaction_amount", precision = 10, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "currency_id")
    private String currencyId;

    @Column(name = "description")
    private String description;

    @Column(name = "collector_id")
    private String collectorId;

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notification_url")
    private String notificationUrl;

    @Column(name = "back_url")
    private String backUrl;

    @Column(name = "failure_message")
    private String failureMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PENDING,
        APPROVED,
        AUTHORIZED,
        IN_PROCESS,
        IN_MEDIATION,
        REJECTED,
        CANCELLED,
        REFUNDED,
        CHARGED_BACK
    }
}
