package com.example.gym.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GoogleSheetsPreviewResponse {
    private String sheetType; // "payments" or "plans"
    private List<String> headers;
    private List<Map<String, String>> rows; // first N rows mapped by column header
    private List<String> warnings;
}
