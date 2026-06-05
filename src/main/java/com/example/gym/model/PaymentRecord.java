package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
@org.hibernate.annotations.Filter(name = "tenantFilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = true)
    private MembershipPlan plan;

    // Snapshot del plan al momento del pago — persiste aunque el plan se elimine
    @Column(name = "plan_name_snapshot", length = 200)
    private String planNameSnapshot;

    @Column(name = "plan_price_snapshot")
    private Double planPriceSnapshot;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.MP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.APPROVED;

    @Column(name = "mp_payment_id", length = 100)
    private String mpPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_id")
    private User registeredBy;

    // Fecha real del pago (puede diferir de createdAt si se registra después)
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PaymentMethod {
        MP, CASH, TRANSFER
    }

    public enum PaymentStatus {
        APPROVED, PENDING, REJECTED
    }
}
