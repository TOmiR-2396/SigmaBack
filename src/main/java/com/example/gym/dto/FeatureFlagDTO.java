package com.example.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un feature flag del tenant actual.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagDTO {
    private String key;
    private boolean enabled;
    private String payload; // JSON opcional
}
