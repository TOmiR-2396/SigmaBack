package com.example.gym.controller;

import com.example.gym.model.MembershipPlan;
import com.example.gym.model.PaymentRecord;
import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import com.example.gym.repository.MembershipPlanRepository;
import com.example.gym.repository.PaymentRecordRepository;
import com.example.gym.repository.SubscriptionRepository;
import com.example.gym.repository.UserRepository;
import com.example.gym.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/membership-plans")
public class MembershipPlanController {
    // Obtener membresía activa de un usuario por su id, con tiempo restante
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMembershipByUserId(@PathVariable("userId") Long userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findActiveByUserId(userId);
        if (subscriptionOpt.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        Subscription sub = subscriptionOpt.get();
        MembershipPlan plan = sub.getPlan();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate endDate = sub.getEndDate();
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);

        java.util.Map<String, Object> planInfo = new java.util.HashMap<>();
        planInfo.put("id",          plan.getId());
        planInfo.put("name",        plan.getName());
        planInfo.put("price",       plan.getPrice());
        planInfo.put("daysPerWeek", plan.getDaysPerWeek());
        planInfo.put("durationMonths", plan.getDurationMonths());

        java.util.Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("plan",       planInfo);
        dto.put("startDate",  sub.getStartDate().toString());
        dto.put("expiresAt",  endDate.toString());
        dto.put("status",     sub.getStatus().name());
        dto.put("daysLeft",   daysLeft > 0 ? daysLeft : 0);
        dto.put("connected",  true);
        return ResponseEntity.ok(dto);
    }

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    @Autowired
    private PaymentService paymentService;

    // Crear membresía (solo OWNER)
    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody com.example.gym.dto.MembershipPlanDTO planDto, Authentication auth) {
        Optional<User> ownerOpt = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst();
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Solo OWNER puede crear membresías");
        }
        MembershipPlan plan = new MembershipPlan();
        plan.setName(planDto.name);
        plan.setDurationMonths(planDto.durationMonths);
        plan.setPrice(planDto.price);
        // daysPerWeek es opcional — null o 0 significa sin restricción semanal
        if (planDto.daysPerWeek != null && planDto.daysPerWeek > 0) {
            plan.setDaysPerWeek(planDto.daysPerWeek);
        }
        membershipPlanRepository.save(plan);
        com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.durationMonths = plan.getDurationMonths();
        dto.price = plan.getPrice();
        dto.daysPerWeek = plan.getDaysPerWeek();
        return ResponseEntity.ok(dto);
    }

    // Listar todos los planes
    @GetMapping
    public List<com.example.gym.dto.MembershipPlanDTO> getAllPlans() {
        List<MembershipPlan> plans = membershipPlanRepository.findAll();
        java.util.List<com.example.gym.dto.MembershipPlanDTO> dtos = new java.util.ArrayList<>();
        for (MembershipPlan plan : plans) {
            com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
            dto.id = plan.getId();
            dto.name = plan.getName();
            dto.durationMonths = plan.getDurationMonths();
            dto.price = plan.getPrice();
            dto.daysPerWeek = plan.getDaysPerWeek();
            dtos.add(dto);
        }
        return dtos;
    }

    // Obtener plan por id
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlan(@PathVariable("id") Long id) {
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MembershipPlan plan = planOpt.get();
        com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.durationMonths = plan.getDurationMonths();
        dto.price = plan.getPrice();
    dto.daysPerWeek = plan.getDaysPerWeek();
        return ResponseEntity.ok(dto);
    }

    // Editar plan (solo OWNER/ADMIN)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable("id") Long id,
                                        @RequestBody com.example.gym.dto.MembershipPlanDTO planDto) {
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(id);
        if (planOpt.isEmpty()) return ResponseEntity.notFound().build();
        MembershipPlan plan = planOpt.get();
        if (planDto.name != null && !planDto.name.isBlank()) plan.setName(planDto.name);
        if (planDto.price != null)         plan.setPrice(planDto.price);
        if (planDto.durationMonths != null) plan.setDurationMonths(planDto.durationMonths);
        if (planDto.daysPerWeek != null && planDto.daysPerWeek > 0) {
            plan.setDaysPerWeek(planDto.daysPerWeek);
        } else if (planDto.daysPerWeek != null && planDto.daysPerWeek == 0) {
            plan.setDaysPerWeek(null); // 0 = sin restricción
        }
        membershipPlanRepository.save(plan);
        com.example.gym.dto.MembershipPlanDTO dto = new com.example.gym.dto.MembershipPlanDTO();
        dto.id = plan.getId(); dto.name = plan.getName();
        dto.durationMonths = plan.getDurationMonths(); dto.price = plan.getPrice();
        dto.daysPerWeek = plan.getDaysPerWeek();
        return ResponseEntity.ok(dto);
    }

    // Eliminar plan (solo OWNER) — desvincula suscripciones y pagos antes de borrar
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deletePlan(@PathVariable("id") Long id) {
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String planName = planOpt.get().getName();
        double planPrice = planOpt.get().getPrice() != null ? planOpt.get().getPrice() : 0;
        // Desvincular suscripciones (guardar snapshot del nombre)
        subscriptionRepository.findAll().stream()
                .filter(s -> s.getPlan() != null && s.getPlan().getId().equals(id))
                .forEach(s -> {
                    s.setPlanNameSnapshot(planName);
                    s.setPlan(null);
                    subscriptionRepository.save(s);
                });
        // Desvincular registros de pago (guardar snapshot)
        paymentRecordRepository.findAll().stream()
                .filter(r -> r.getPlan() != null && r.getPlan().getId().equals(id))
                .forEach(r -> {
                    r.setPlanNameSnapshot(planName);
                    r.setPlanPriceSnapshot(planPrice);
                    r.setPlan(null);
                    paymentRecordRepository.save(r);
                });
        membershipPlanRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Plan eliminado correctamente"));
    }

    // Cancelar suscripción activa de un usuario (solo OWNER/ADMIN)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @DeleteMapping("/subscriptions/{userId}")
    public ResponseEntity<?> cancelUserSubscription(@PathVariable("userId") Long userId) {
        Optional<Subscription> subOpt = subscriptionRepository.findActiveByUserId(userId);
        if (subOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("El usuario no tiene suscripción activa");
        }
        Subscription sub = subOpt.get();
        sub.setStatus(Subscription.Status.CANCELED);
        subscriptionRepository.save(sub);
        return ResponseEntity.ok(Map.of("message", "Suscripción cancelada correctamente"));
    }

    // Asignar membresía a usuario (solo OWNER)
    @PostMapping("/assign")
    public ResponseEntity<?> assignPlanToUser(@RequestParam("userId") Long userId,
                                              @RequestParam("planId") Long planId,
                                              Authentication auth) {
        // Validar que el usuario autenticado sea OWNER
        // Buscar usuario OWNER autenticado por rol
        Optional<User> ownerOpt = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.OWNER)
            .findFirst();
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Solo OWNER puede asignar membresías");
        }

        // Buscar usuario y plan
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<MembershipPlan> planOpt = membershipPlanRepository.findById(planId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Usuario no encontrado");
        }
        if (planOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Plan de membresía no encontrado");
        }
        // Validar que el usuario destino sea MEMBER
        if (userOpt.get().getRole() != User.UserRole.MEMBER) {
            return ResponseEntity.badRequest().body("Solo se puede asignar membresía a usuarios con rol MEMBER");
        }

        // Cancelar suscripciones activas previas
        paymentService.cancelActiveSubscriptions(userOpt.get());

        // Crear nueva suscripción
        Subscription subscription = new Subscription();
        subscription.setUser(userOpt.get());
        subscription.setPlan(planOpt.get());
        subscription.setStartDate(java.time.LocalDate.now());
        Integer duration = planOpt.get().getDurationMonths();
        java.time.LocalDate endDate = (duration == null || duration == 0)
                ? java.time.LocalDate.now().plusDays(7)
                : java.time.LocalDate.now().plusMonths(duration);
        subscription.setEndDate(endDate);
        subscription.setStatus(Subscription.Status.ACTIVE);
        subscriptionRepository.save(subscription);

        // Guardar registro de asignación manual (sin pago registrado)
        PaymentRecord record = PaymentRecord.builder()
                .user(userOpt.get())
                .plan(planOpt.get())
                .amount(0.0)
                .method(PaymentRecord.PaymentMethod.CASH)
                .status(PaymentRecord.PaymentStatus.APPROVED)
                .build();
        paymentRecordRepository.save(record);

        return ResponseEntity.ok(Map.of("message", "Membresía asignada correctamente", "subscriptionId", subscription.getId()));
    }

    /**
     * Registra un pago en efectivo y activa la suscripción (OWNER/TRAINER).
     */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    @PostMapping("/assign-cash")
    public ResponseEntity<?> registerCashPayment(
            @RequestParam("userId") Long userId,
            @RequestParam("planId") Long planId,
            @RequestParam(value = "paymentDate", required = false) String paymentDateStr,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "method", required = false, defaultValue = "CASH") String methodStr,
            Authentication auth) {
        try {
            User registeredBy = (User) auth.getPrincipal();
            LocalDate paymentDate = paymentDateStr != null ? LocalDate.parse(paymentDateStr) : null;
            PaymentRecord.PaymentMethod method;
            try {
                method = PaymentRecord.PaymentMethod.valueOf(methodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Método inválido. Valores aceptados: CASH, TRANSFER, MP");
            }
            Subscription sub = paymentService.registerCashPayment(userId, planId, registeredBy,
                                                                   paymentDate, notes, method);
            return ResponseEntity.ok(Map.of(
                "message", "Pago registrado correctamente",
                "subscriptionId", sub.getId(),
                "endDate", sub.getEndDate()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error registrando pago: " + e.getMessage());
        }
    }

    /**
     * Historial de pagos del usuario logueado.
     */
    @GetMapping("/payments/history/me")
    public ResponseEntity<?> getMyPaymentHistory(Authentication auth) {
        User user = (User) auth.getPrincipal();
        List<PaymentRecord> records = paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(records.stream().map(this::toPaymentDto).collect(Collectors.toList()));
    }

    /**
     * Historial de pagos de un usuario específico (OWNER/TRAINER).
     */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    @GetMapping("/payments/history/{userId}")
    public ResponseEntity<?> getUserPaymentHistory(@PathVariable Long userId) {
        List<PaymentRecord> records = paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(records.stream().map(this::toPaymentDto).collect(Collectors.toList()));
    }

    private java.util.Map<String, Object> toPaymentDto(PaymentRecord r) {
        java.util.Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", r.getId());
        dto.put("amount", r.getAmount());
        dto.put("method", r.getMethod().name());
        dto.put("status", r.getStatus().name());
        dto.put("mpPaymentId", r.getMpPaymentId());
        dto.put("createdAt", r.getCreatedAt());
        dto.put("paymentDate", r.getPaymentDate() != null ? r.getPaymentDate().toString()
                             : r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : null);
        dto.put("notes", r.getNotes());
        dto.put("planName",  r.getPlan() != null ? r.getPlan().getName()
                           : r.getPlanNameSnapshot()  != null ? r.getPlanNameSnapshot()  : "-");
        dto.put("planPrice", r.getPlan() != null ? r.getPlan().getPrice()
                           : r.getPlanPriceSnapshot() != null ? r.getPlanPriceSnapshot() : r.getAmount());
        dto.put("registeredBy", r.getRegisteredBy() != null ? r.getRegisteredBy().getEmail() : null);
        return dto;
    }
}
