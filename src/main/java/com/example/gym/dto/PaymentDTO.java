package com.example.gym.dto;

import com.example.gym.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long id;
    private String preferenceId;
    private String paymentId;
    private String paymentMethodId;
    private String paymentTypeId;
    private Long userId;
    private String userName;
    private Long membershipPlanId;
    private String membershipPlanName;
    private BigDecimal amount;
    private Payment.PaymentStatus status;
    private String externalReference;
    private BigDecimal transactionAmount;
    private String currencyId;
    private String description;
    private String payerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private String failureMessage;
}
