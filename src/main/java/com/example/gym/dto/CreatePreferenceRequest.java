package com.example.gym.dto;

/**
 * DTO simple para crear una Preference (Wallet Brick).
 * Podés ampliarlo según tus reglas de negocio.
 */
public class CreatePreferenceRequest {
    public String title;            // título del ítem
    public Integer quantity;        // cantidad
    public Double unitPrice;        // precio unitario
    public String currency;         // ISO, ej: ARS
    public String payerEmail;       // email del pagador (opcional)
    public String externalReference;// referencia interna (opcional)

    // getters/setters opcionales si fuera necesario
}
