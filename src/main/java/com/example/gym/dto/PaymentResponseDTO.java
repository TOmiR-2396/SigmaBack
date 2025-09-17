package com.example.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    private String externalReference;
    private String message;
    private boolean success;
}
