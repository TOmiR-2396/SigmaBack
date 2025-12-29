package com.example.gym.service;

import com.example.gym.dto.MembershipInfoDTO;
import com.example.gym.dto.MembershipPlanDTO;
import com.example.gym.model.MembershipPlan;
import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import com.example.gym.repository.MembershipPlanRepository;
import com.example.gym.repository.SubscriptionRepository;
import com.example.gym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MembershipService: Gestión de planes de membresía y suscripciones
 * Refactorización del código disperso en MembershipPlanController
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    // ============ PLANES DE MEMBRESÍA ============

    /**
     * Crear nuevo plan de membresía (solo OWNER)
     */
    public MembershipPlanDTO createPlan(MembershipPlanDTO planDto, User currentUser) {
        log.info("Creating membership plan: {}", planDto.name);

        // Validar que sea OWNER
        if (currentUser.getRole() != User.UserRole.OWNER) {
            throw new IllegalArgumentException("Solo OWNER puede crear planes de membresía");
        }

        // Validaciones
        if (planDto.name == null || planDto.name.isBlank()) {
            throw new IllegalArgumentException("Nombre de plan requerido");
        }
        if (planDto.durationMonths == null || planDto.durationMonths < 1) {
            throw new IllegalArgumentException("Duración en meses debe ser positivo");
        }
        if (planDto.price == null || planDto.price < 0) {
            throw new IllegalArgumentException("Precio debe ser positivo");
        }
        if (planDto.daysPerWeek == null || planDto.daysPerWeek < 1 || planDto.daysPerWeek > 7) {
            throw new IllegalArgumentException("Días por semana debe estar entre 1 y 7");
        }

        // Validación específica: Funcional Kids
        if (planDto.name.equalsIgnoreCase("Funcional Kids") && !planDto.daysPerWeek.equals(2)) {
            throw new IllegalArgumentException("Funcional Kids solo puede ser de 2 días a la semana");
        }

        MembershipPlan plan = MembershipPlan.builder()
                .name(planDto.name)
                .durationMonths(planDto.durationMonths)
                .price(planDto.price)
                .daysPerWeek(planDto.daysPerWeek)
                .build();

        MembershipPlan saved = membershipPlanRepository.save(plan);
        log.info("✓ Plan creado: {} (id={})", saved.getName(), saved.getId());

        return mapPlanToDTO(saved);
    }

    /**
     * Obtener todos los planes
     */
    public List<MembershipPlanDTO> getAllPlans() {
        log.debug("Getting all membership plans");
        return membershipPlanRepository.findAll()
                .stream()
                .map(this::mapPlanToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener plan por ID
     */
    public MembershipPlanDTO getPlanById(Long planId) {
        log.debug("Getting plan: {}", planId);
        return membershipPlanRepository.findById(planId)
                .map(this::mapPlanToDTO)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + planId));
    }

    /**
     * Actualizar plan (solo OWNER)
     */
    public MembershipPlanDTO updatePlan(Long planId, MembershipPlanDTO planDto, User currentUser) {
        log.info("Updating plan: {}", planId);

        if (currentUser.getRole() != User.UserRole.OWNER) {
            throw new IllegalArgumentException("Solo OWNER puede actualizar planes");
        }

        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));

        // Validaciones
        if (planDto.durationMonths != null && planDto.durationMonths < 1) {
            throw new IllegalArgumentException("Duración debe ser positiva");
        }
        if (planDto.price != null && planDto.price < 0) {
            throw new IllegalArgumentException("Precio debe ser positivo");
        }

        // Actualizar campos
        if (planDto.name != null && !planDto.name.isBlank()) {
            plan.setName(planDto.name);
        }
        if (planDto.durationMonths != null) {
            plan.setDurationMonths(planDto.durationMonths);
        }
        if (planDto.price != null) {
            plan.setPrice(planDto.price);
        }
        if (planDto.daysPerWeek != null) {
            plan.setDaysPerWeek(planDto.daysPerWeek);
        }

        MembershipPlan updated = membershipPlanRepository.save(plan);
        log.info("✓ Plan actualizado: {}", updated.getId());

        return mapPlanToDTO(updated);
    }

    /**
     * Eliminar plan (solo OWNER)
     */
    public void deletePlan(Long planId, User currentUser) {
        log.info("Deleting plan: {}", planId);

        if (currentUser.getRole() != User.UserRole.OWNER) {
            throw new IllegalArgumentException("Solo OWNER puede eliminar planes");
        }

        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));

        // Verificar si hay suscripciones activas
        long activeSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPlan().getId().equals(planId) && s.getStatus() == Subscription.Status.ACTIVE)
                .count();

        if (activeSubscriptions > 0) {
            throw new IllegalArgumentException("No se puede eliminar un plan con suscripciones activas");
        }

        membershipPlanRepository.delete(plan);
        log.info("✓ Plan eliminado: {}", planId);
    }

    // ============ SUSCRIPCIONES ============

    /**
     * Asignar membresía a usuario (crear suscripción)
     */
    public void assignPlanToUser(Long userId, Long planId, User currentUser) {
        log.info("Assigning plan {} to user {}", planId, userId);

        if (currentUser.getRole() != User.UserRole.OWNER) {
            throw new IllegalArgumentException("Solo OWNER puede asignar membresías");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getRole() != User.UserRole.MEMBER) {
            throw new IllegalArgumentException("Solo se puede asignar membresía a usuarios MEMBER");
        }

        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));

        // Cancelar suscripción anterior si existe
        subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
                .forEach(s -> {
                    s.setStatus(Subscription.Status.CANCELED);
                    subscriptionRepository.save(s);
                    log.info("✓ Suscripción anterior cancelada: {}", s.getId());
                });

        // Crear nueva suscripción
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(plan.getDurationMonths()))
                .status(Subscription.Status.ACTIVE)
                .build();

        subscriptionRepository.save(subscription);
        log.info("✓ Membresía asignada: user={}, plan={}, endDate={}", 
                userId, planId, subscription.getEndDate());
    }

    /**
     * Obtener membresía activa del usuario
     */
    public MembershipInfoDTO getActiveMembership(Long userId) {
        log.debug("Getting active membership for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Subscription subscription = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId) && s.getStatus() == Subscription.Status.ACTIVE)
                .findFirst()
                .orElse(null);

        if (subscription == null) {
            log.debug("No active membership for user: {}", userId);
            return null;
        }

        return mapSubscriptionToDTO(subscription);
    }

    /**
     * Cancelar membresía
     */
    public void cancelMembership(Long subscriptionId, User currentUser) {
        log.info("Cancelling membership: {}", subscriptionId);

        if (currentUser.getRole() != User.UserRole.OWNER) {
            throw new IllegalArgumentException("Solo OWNER puede cancelar membresías");
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Suscripción no encontrada"));

        subscription.setStatus(Subscription.Status.CANCELED);
        subscriptionRepository.save(subscription);
        log.info("✓ Membresía cancelada: {}", subscriptionId);
    }

    // ============ HELPERS ============

    private MembershipPlanDTO mapPlanToDTO(MembershipPlan plan) {
        return MembershipPlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .durationMonths(plan.getDurationMonths())
                .price(plan.getPrice())
                .daysPerWeek(plan.getDaysPerWeek())
                .build();
    }

    private MembershipInfoDTO mapSubscriptionToDTO(Subscription subscription) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = subscription.getEndDate();
        long daysLeft = ChronoUnit.DAYS.between(today, endDate);

        return MembershipInfoDTO.builder()
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .durationMonths(subscription.getPlan().getDurationMonths())
                .price(subscription.getPlan().getPrice())
                .daysPerWeek(subscription.getPlan().getDaysPerWeek())
                .startDate(subscription.getStartDate())
                .endDate(endDate)
                .status(subscription.getStatus().name())
                .daysLeft(Math.max(daysLeft, 0))
                .build();
    }
}
