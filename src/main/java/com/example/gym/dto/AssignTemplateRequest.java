package com.example.gym.dto;

public class AssignTemplateRequest {
    private Long templateId;
    private Long userId;

    // Constructor por defecto
    public AssignTemplateRequest() {}

    // Constructor con parámetros
    public AssignTemplateRequest(Long templateId, Long userId) {
        this.templateId = templateId;
        this.userId = userId;
    }

    // Getters y Setters
    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
