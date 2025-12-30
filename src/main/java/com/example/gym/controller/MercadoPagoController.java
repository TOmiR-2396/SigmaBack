package com.example.gym.controller;

import com.example.gym.dto.CreatePreferenceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoints para integrar Wallet Brick y recibir Webhooks de Mercado Pago.
 */
@RestController
public class MercadoPagoController {
    
    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoController.class);

    @Value("${mercadopago.public-key:}")
    private String publicKey;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${mercadopago.webhook-strict:false}")
    private boolean webhookStrict;

    @Value("${app.frontend.reset-url:http://localhost:5173/}")
    private String frontendBase;

    /**
     * Crear Preference para Wallet Brick.
     * Frontend debe invocar este endpoint y luego inicializar el Brick con el preferenceId.
     */
    @PostMapping("/api/mp/preference")
    public ResponseEntity<?> createPreference(@RequestBody CreatePreferenceRequest req) {
        try {
            if (req == null || req.unitPrice == null || req.unitPrice <= 0 || req.quantity == null || req.quantity <= 0) {
                return ResponseEntity.badRequest().body("Monto o cantidad inválidos");
            }

            // Construir payload JSON manualmente para la API REST
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> item = Map.of(
                "title", req.title != null ? req.title : "SigmaGym - Membresía",
                "quantity", req.quantity,
                "unit_price", req.unitPrice,
                "currency_id", req.currency != null ? req.currency : "ARS"
            );
            Map<String, Object> backUrls = Map.of(
                "success", frontendBase,
                "pending", frontendBase,
                "failure", frontendBase
            );
            Map<String, Object> preferenceBody = Map.of(
                "items", List.of(item),
                "back_urls", backUrls,
                "external_reference", req.externalReference,
                "payer", (req.payerEmail != null && !req.payerEmail.isBlank()) ? Map.of("email", req.payerEmail) : null
            );

            String json = mapper.writeValueAsString(preferenceBody);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/checkout/preferences"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> respBody = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>(){});
                Map<String, Object> resp = Map.of(
                    "preferenceId", respBody.get("id"),
                    "initPoint", respBody.get("init_point"),
                    "sandboxInitPoint", respBody.get("sandbox_init_point"),
                    "publicKey", publicKey
                );
                return ResponseEntity.ok(resp);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("MP API error: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creando preference: " + e.getMessage());
        }
    }

    /**
     * Webhook para notificaciones de MP.
     * URL local de prueba sugerida: http://localhost:8080/webhooks/mercadopago
     */
    @PostMapping("/webhooks/mercadopago")
    public ResponseEntity<?> webhook(HttpServletRequest request,
                                     @RequestBody Map<String, Object> body,
                                     @RequestParam(value = "type", required = false) String type,
                                     @RequestHeader(value = "x-signature", required = false) String signatureHeader) {
        try {
            // Validación opcional de firma del webhook
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                String rawBody = extractRawBody(request);
                boolean signatureOk = validateSignature(signatureHeader, webhookSecret, request.getRequestURI(), rawBody);
                if (!signatureOk) {
                    logger.warn("Firma de webhook inválida o ausente.");
                    if (webhookStrict) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid webhook signature");
                    }
                }
            }
            // Mercado Pago suele enviar {
            //   "type": "payment",
            //   "data": { "id": "<paymentId>" }
            // }
            String eventType = type;
            if (eventType == null && body != null && body.get("type") instanceof String) {
                eventType = (String) body.get("type");
            }

            if ("payment".equalsIgnoreCase(eventType)) {
                Object dataObj = body.get("data");
                if (dataObj instanceof Map) {
                    Object idObj = ((Map<?, ?>) dataObj).get("id");
                    if (idObj != null) {
                        String paymentIdStr = String.valueOf(idObj);
                        // Consultar pago vía API REST
                        HttpRequest reqPayment = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentIdStr))
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();
                        HttpClient client = HttpClient.newHttpClient();
                        HttpResponse<String> resp = client.send(reqPayment, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            ObjectMapper mapper = new ObjectMapper();
                            Map<String, Object> payment = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});
                            // TODO: Actualizar tu estado interno: membresía, plan, orden, etc.
                            return ResponseEntity.ok(Map.of(
                                "received", true,
                                "paymentId", payment.get("id"),
                                "status", payment.get("status"),
                                "detail", payment.get("status_detail")
                            ));
                        }
                    }
                }
            }

            // Si no se pudo identificar o no es de interés, igual respondemos 200
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            // MP reintenta en errores; intenta responder rápido 200 cuando sea posible
            return ResponseEntity.ok(Map.of("received", true));
        }
    }

    /**
     * Extrae el cuerpo crudo del request para validar firma.
     */
    private String extractRawBody(HttpServletRequest request) {
        try {
            if (request instanceof ContentCachingRequestWrapper) {
                byte[] content = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
                return content != null ? new String(content, StandardCharsets.UTF_8) : "";
            }
        } catch (Exception ignore) {}
        return "";
    }

    /**
     * Validación de firma del webhook.
     * Formato esperado de header: "ts=<timestamp>, v1=<hex>".
     * Cálculo: HMAC-SHA256(secret, ts + ":" + path + ":" + body) == v1.
     */
    private boolean validateSignature(String signatureHeader, String secret, String path, String body) {
        try {
            if (signatureHeader == null || signatureHeader.isBlank()) return false;
            Map<String, String> parts = parseSignatureHeader(signatureHeader);
            String ts = parts.getOrDefault("ts", "");
            String v1 = parts.getOrDefault("v1", "").toLowerCase();
            if (ts.isBlank() || v1.isBlank()) return false;

            String message = ts + ":" + path + ":" + (body != null ? body : "");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(digest).toLowerCase();
            return expected.equals(v1);
        } catch (Exception e) {
            logger.warn("Error validando firma: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, String> parseSignatureHeader(String header) {
        Map<String, String> map = new HashMap<>();
        try {
            String[] tokens = header.split(",|&");
            for (String token : tokens) {
                String[] kv = token.trim().split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
        } catch (Exception ignore) {}
        return map;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
