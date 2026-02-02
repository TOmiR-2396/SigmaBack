package com.example.gym.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipDTO {
    private Long id;
    private String title;
    private String content;
    private String emoji;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByEmail;
}
