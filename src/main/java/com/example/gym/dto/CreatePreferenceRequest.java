package com.example.gym.dto;

/**
 * DTO simple para crear una Preference (Wallet Brick).
 * Podés ampliarlo según tus reglas de negocio.
 */
public class CreatePreferenceRequest {
    public String title;
    public Integer quantity;
    public Double unitPrice;
    public String currency;
    public String payerEmail;
    public String externalReference;
    /** Si se envía planId + userId, el backend busca el precio real y construye el external_reference automáticamente. */
    public Long planId;
    public Long userId;

    // getters/setters opcionales si fuera necesario
}
