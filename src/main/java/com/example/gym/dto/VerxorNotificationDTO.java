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
public class VerxorNotificationDTO {
    private String paymentId;
    private String status;
    private String externalReference;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String timestamp;
    private String projectId;
    private PaymentData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {
        private String id;
        private String status;
        private String statusDetail;
        private String paymentMethodId;
        private String paymentTypeId;
        private BigDecimal transactionAmount;
        private String currencyId;
        private String description;
        private String externalReference;
        private PayerInfo payer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayerInfo {
        private String email;
        private String firstName;
        private String lastName;
        private String identificationType;
        private String identificationNumber;
    }
}
