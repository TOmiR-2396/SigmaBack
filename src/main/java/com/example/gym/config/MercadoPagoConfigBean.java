package com.example.gym.config;

import com.mercadopago.MercadoPagoConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Configuración central de Mercado Pago: setea el Access Token al inicio.
 * Las credenciales se definen en application.yml y se inyectan por @Value.
 */
@Configuration
public class MercadoPagoConfigBean {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isBlank()) {
            // En dev podés no setear el token; los endpoints devolverán error claro
            System.out.println("[MercadoPago] Access Token no configurado. Setea MP_ACCESS_TOKEN en el entorno.");
        } else {
            MercadoPagoConfig.setAccessToken(accessToken);
            System.out.println("[MercadoPago] Access Token configurado.");
        }
    }
}
