package com.example.gym.controller;

import com.example.gym.model.PaymentRecord;
import com.example.gym.model.User;
import com.example.gym.repository.PaymentRecordRepository;
import com.example.gym.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Resumen mensual de cuotas — todos los miembros con su estado de pago.
     * Equivale a la planilla CUOTAS del mes.
     *
     * GET /api/payments/monthly-summary?year=2026&month=6
     */
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    @GetMapping("/monthly-summary")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {

        List<PaymentRecord> payments = paymentRecordRepository.findApprovedByMonth(year, month);
        // userId → pago más reciente del mes (un miembro puede tener más de un registro)
        Map<Long, PaymentRecord> paymentByUser = new LinkedHashMap<>();
        for (PaymentRecord r : payments) {
            Long uid = r.getUser().getId();
            if (!paymentByUser.containsKey(uid)) {
                paymentByUser.put(uid, r);
            }
        }

        List<User> members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.MEMBER)
                .sorted(Comparator.comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER)
                                  .thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (User member : members) {
            PaymentRecord p = paymentByUser.get(member.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId",    member.getId());
            row.put("lastName",  member.getLastName());
            row.put("firstName", member.getFirstName());
            row.put("hasPaid",   p != null);
            row.put("amount",    p != null ? p.getAmount() : null);
            row.put("method",    p != null ? p.getMethod().name() : null);
            row.put("paymentDate", p != null
                    ? (p.getPaymentDate() != null ? p.getPaymentDate().toString()
                                                  : p.getCreatedAt().toLocalDate().toString())
                    : null);
            row.put("notes",     p != null ? p.getNotes() : null);
            row.put("planName",  p != null
                    ? (p.getPlan() != null ? p.getPlan().getName()
                                           : p.getPlanNameSnapshot() != null ? p.getPlanNameSnapshot() : "-")
                    : null);
            result.add(row);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("year",  year);
        response.put("month", month);
        response.put("total", members.size());
        response.put("paid",  paymentByUser.size());
        response.put("pending", members.size() - paymentByUser.size());
        response.put("members", result);
        return ResponseEntity.ok(response);
    }
}
