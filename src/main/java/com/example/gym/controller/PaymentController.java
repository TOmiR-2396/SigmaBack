package com.example.gym.controller;

import com.example.gym.dto.*;
import com.example.gym.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-preference")
    @PreAuthorize("hasRole('MEMBER') or hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<PaymentResponseDTO> createPaymentPreference(
            @Valid @RequestBody PaymentRequestDTO request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            PaymentResponseDTO response = paymentService.createPaymentPreference(request, userEmail);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error creando preferencia de pago: {}", e.getMessage());
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .message("Error interno del servidor: " + e.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleVerxorWebhook(@RequestBody Map<String, Object> notification) {
        try {
            log.info("Recibida notificación de Verxor: {}", notification);
            
            // Extraer el ID del pago desde la notificación de Verxor
            Object paymentIdObj = notification.get("payment_id");
            if (paymentIdObj != null) {
                String paymentId = paymentIdObj.toString();
                paymentService.processPaymentNotification(paymentId);
            } else {
                log.warn("No se encontró payment_id en la notificación de Verxor");
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error procesando webhook de Verxor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('MEMBER') or hasRole('TRAINER') or hasRole('OWNER')")
    public ResponseEntity<List<PaymentDTO>> getMyPayments(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<PaymentDTO> payments = paymentService.getUserPayments(userEmail);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error obteniendo pagos del usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        try {
            List<PaymentDTO> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error obteniendo todos los pagos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/preference/{preferenceId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PaymentDTO> getPaymentByPreferenceId(@PathVariable String preferenceId) {
        try {
            return paymentService.getPaymentByPreferenceId(preferenceId)
                    .map(payment -> ResponseEntity.ok(payment))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error obteniendo pago por preference ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> handlePaymentSuccess(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String external_reference) {
        
        log.info("Pago exitoso - Payment ID: {}, Status: {}, External Reference: {}", 
                payment_id, status, external_reference);
        
        Map<String, Object> response = Map.of(
                "message", "Pago procesado exitosamente",
                "paymentId", payment_id != null ? payment_id : "",
                "status", status != null ? status : "",
                "externalReference", external_reference != null ? external_reference : ""
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> handlePaymentFailure(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String external_reference) {
        
        log.warn("Pago fallido - Payment ID: {}, Status: {}, External Reference: {}", 
                payment_id, status, external_reference);
        
        Map<String, Object> response = Map.of(
                "message", "El pago no pudo ser procesado",
                "paymentId", payment_id != null ? payment_id : "",
                "status", status != null ? status : "",
                "externalReference", external_reference != null ? external_reference : ""
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> handlePaymentPending(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String external_reference) {
        
        log.info("Pago pendiente - Payment ID: {}, Status: {}, External Reference: {}", 
                payment_id, status, external_reference);
        
        Map<String, Object> response = Map.of(
                "message", "El pago está pendiente de aprobación",
                "paymentId", payment_id != null ? payment_id : "",
                "status", status != null ? status : "",
                "externalReference", external_reference != null ? external_reference : ""
        );
        
        return ResponseEntity.ok(response);
    }
}
