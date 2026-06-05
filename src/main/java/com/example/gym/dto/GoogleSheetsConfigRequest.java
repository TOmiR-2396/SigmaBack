package com.example.gym.dto;

import lombok.Data;

@Data
public class GoogleSheetsConfigRequest {
    private String paymentsSheetUrl;
    private String plansSheetUrl;
    private boolean syncPayments;
    private boolean syncPlans;
}
