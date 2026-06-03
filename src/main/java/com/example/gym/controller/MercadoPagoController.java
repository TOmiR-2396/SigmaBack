package com.example.gym.controller;

import com.example.gym.dto.CreatePreferenceRequest;
import com.example.gym.model.MembershipPlan;
import com.example.gym.repository.MembershipPlanRepository;
import com.example.gym.service.MercadoPagoCredentialService;
import com.example.gym.service.PaymentService;
import com.example.gym.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private MercadoPagoCredentialService mpCredentials;

    @Value("${mercadopago.webhook-strict:false}")
    private boolean webhookStrict;

    @Value("${mercadopago.frontend-url:http://localhost:5173}")
    private String frontendBase;

    /**
     * Procesa un pago enviado por el Payment Brick (Checkout API / Transparent Checkout).
     * El Brick tokeniza los datos de la tarjeta y envía el token + metadata acá.
     * El backend llama a POST /v1/payments en MP y activa la suscripción si el pago es aprobado.
     */
    @PostMapping("/api/payments/process")
    public ResponseEntity<?> processPayment(
            @RequestBody Map<String, Object> body,
            org.springframework.security.core.Authentication authentication) {
        try {
            com.example.gym.model.User user = (com.example.gym.model.User) authentication.getPrincipal();

            Object planIdObj = body.get("planId");
            Object paymentData = body.get("paymentData");
            if (planIdObj == null || !(paymentData instanceof Map)) {
                return ResponseEntity.badRequest().body("planId y paymentData son requeridos");
            }

            Long planId = Long.parseLong(String.valueOf(planIdObj));
            com.example.gym.model.MembershipPlan plan = planRepository.findById(planId).orElse(null);
            if (plan == null) {
                return ResponseEntity.badRequest().body("Plan no encontrado: " + planId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> pd = (Map<String, Object>) paymentData;
            Map<String, Object> paymentBody = new HashMap<>(pd);
            double price = plan.getPrice();
            paymentBody.put("transaction_amount", price);
            paymentBody.put("description", "GestiGym - " + plan.getName());

            String tenantId = TenantContext.getCurrentTenant();
            String externalRef = tenantId + ":" + user.getId() + ":" + planId;
            paymentBody.put("external_reference", externalRef);

            // Split 1:1 Marketplace: comisión de gestigym
            double fee = mpCredentials.calculateFee(price);
            if (fee > 0) {
                paymentBody.put("application_fee", fee);
            }

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(paymentBody);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/payments"))
                .header("Authorization", "Bearer " + mpCredentials.getAccessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<String, Object> mpResp = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String status = String.valueOf(mpResp.get("status"));
                String paymentId = String.valueOf(mpResp.get("id"));
                if ("approved".equalsIgnoreCase(status)) {
                    paymentService.activateSubscription(externalRef, paymentId);
                }
                return ResponseEntity.ok(Map.of(
                    "status", status,
                    "paymentId", mpResp.get("id"),
                    "detail", mpResp.getOrDefault("status_detail", "")
                ));
            } else {
                logger.error("[MP/process] Error: status={} body={}", resp.statusCode(), resp.body());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("MP API error: " + resp.statusCode() + " - " + resp.body());
            }
        } catch (Exception e) {
            logger.error("[MP/process] Excepción: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error procesando pago: " + e.getMessage());
        }
    }

    /**
     * Devuelve la public key de MP para que el frontend inicialice el Brick.
     * Accesible también para usuarios INACTIVE (necesitan pagar para activarse).
     */
    @GetMapping("/api/payments/config")
    public ResponseEntity<?> getPaymentsConfig() {
        return ResponseEntity.ok(Map.of("publicKey", mpCredentials.getIntegratorPublicKey()));
    }

    /**
     * Verifica el estado de un pago en Mercado Pago.
     */
    @GetMapping("/api/payments/verify/{paymentId}")
    public ResponseEntity<?> verifyPayment(@PathVariable String paymentId) {
        try {
            HttpRequest reqPayment = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId))
                .header("Authorization", "Bearer " + mpCredentials.getAccessToken())
                .GET()
                .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(reqPayment, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> payment = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});
                return ResponseEntity.ok(Map.of(
                    "id", payment.get("id"),
                    "status", payment.get("status"),
                    "status_detail", payment.get("status_detail"),
                    "transaction_amount", payment.get("transaction_amount"),
                    "external_reference", payment.get("external_reference")
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("MP API error: " + resp.statusCode());
            }
        } catch (Exception e) {
            logger.error("[MP/verify] Error verificando pago {}: {}", paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error verificando pago: " + e.getMessage());
        }
    }

    /**
     * Crear Preference para Wallet Brick.
     * Frontend debe invocar este endpoint y luego inicializar el Brick con el preferenceId.
     */
    @PostMapping("/api/mp/preference")
    public ResponseEntity<?> createPreference(@RequestBody CreatePreferenceRequest req) {
        try {
            if (req == null) {
                return ResponseEntity.badRequest().body("Request inválido");
            }

            // Si vienen planId + userId, tomamos el precio real del plan y armamos el external_reference
            String title = req.title != null ? req.title : "SigmaGym - Membresía";
            double price = req.unitPrice != null ? req.unitPrice : 0;
            String externalRef = req.externalReference;

            if (req.planId != null && req.userId != null) {
                MembershipPlan plan = planRepository.findById(req.planId).orElse(null);
                if (plan != null) {
                    price = plan.getPrice();
                    title = "SigmaGym - " + plan.getName();
                    logger.info("[MP] Plan encontrado: id={} nombre='{}' precio={}", plan.getId(), plan.getName(), plan.getPrice());
                } else {
                    logger.warn("[MP] Plan no encontrado: planId={}", req.planId);
                }
                String tenantId = TenantContext.getCurrentTenant();
                externalRef = tenantId + ":" + req.userId + ":" + req.planId;
                logger.info("[MP] external_reference armado: {}", externalRef);
            } else {
                logger.info("[MP] Sin planId/userId — usando precio manual: {}", price);
            }

            int qty = req.quantity != null ? req.quantity : 1;
            logger.info("[MP] Creando preference — title='{}' price={} qty={}", title, price, qty);
            if (price <= 0 || qty <= 0) {
                logger.warn("[MP] Precio o cantidad inválidos: price={} qty={}", price, qty);
                return ResponseEntity.badRequest().body("Monto o cantidad inválidos: price=" + price + ", qty=" + qty);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> item = Map.of(
                "title", title,
                "quantity", qty,
                "unit_price", price,
                "currency_id", req.currency != null ? req.currency : "ARS"
            );
            String base = frontendBase.replaceAll("/+$", "");
            Map<String, Object> backUrls = Map.of(
                "success", base + "/payment/success",
                "pending", base + "/payment/pending",
                "failure", base + "/payment/failure"
            );
            Map<String, Object> preferenceBody = new HashMap<>();
            preferenceBody.put("items", List.of(item));
            preferenceBody.put("back_urls", backUrls);
            preferenceBody.put("external_reference", externalRef);
            if (req.payerEmail != null && !req.payerEmail.isBlank()) {
                preferenceBody.put("payer", Map.of("email", req.payerEmail));
            }
            // Split 1:1 Marketplace: comisión de gestigym sobre cada pago
            double fee = mpCredentials.calculateFee(price);
            if (fee > 0) {
                preferenceBody.put("marketplace_fee", fee);
            }

            String json = mapper.writeValueAsString(preferenceBody);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/checkout/preferences"))
                .header("Authorization", "Bearer " + mpCredentials.getAccessToken())
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
                    "publicKey", mpCredentials.getIntegratorPublicKey()
                );
                return ResponseEntity.ok(resp);
            } else {
                logger.error("[MP] Error de API: status={} body={}", response.statusCode(), response.body());
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
            String webhookSecret = mpCredentials.getWebhookSecret();
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
                        // Consultar el pago con el token del integrador (siempre puede ver pagos de sellers)
                        String webhookAccessToken = mpCredentials.getAccessToken();

                        HttpRequest reqPayment = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentIdStr))
                            .header("Authorization", "Bearer " + webhookAccessToken)
                            .GET()
                            .build();
                        HttpClient client = HttpClient.newHttpClient();
                        HttpResponse<String> resp = client.send(reqPayment, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            ObjectMapper mapper = new ObjectMapper();
                            Map<String, Object> payment = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});
                            String status = String.valueOf(payment.get("status"));
                            String externalRef = payment.get("external_reference") instanceof String
                                    ? (String) payment.get("external_reference") : null;

                            if ("approved".equalsIgnoreCase(status)) {
                                paymentService.activateSubscription(externalRef, paymentIdStr);
                            } else {
                                logger.info("[Webhook] Pago {} con status '{}' — no se activa suscripción.", paymentIdStr, status);
                            }

                            return ResponseEntity.ok(Map.of(
                                "received", true,
                                "paymentId", payment.get("id"),
                                "status", status,
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

    /** Intenta extraer el tenantId del external_reference que viene en el body del webhook. */
    private String extractTenantFromBody(Map<String, Object> body) {
        try {
            Object data = body.get("data");
            if (data instanceof Map) {
                Object extRef = ((Map<?, ?>) data).get("external_reference");
                if (extRef instanceof String ref && ref.contains(":")) {
                    return ref.split(":", 2)[0];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
