package com.example.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private Long membershipPlanId;
    private String description;
    private String payerEmail;
    private String externalReference;
    private BigDecimal amount;
}
