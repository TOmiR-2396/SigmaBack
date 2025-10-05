package com.example.gym.service;

import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import com.example.gym.repository.SubscriptionRepository;
import com.example.gym.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MembershipExpirationService {

    private static final Logger logger = LoggerFactory.getLogger(MembershipExpirationService.class);

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Job programado que se ejecuta todos los días a las 02:00 AM
     * para verificar y desactivar usuarios con membresías vencidas
     */
    @Scheduled(cron = "0 0 2 * * *") // Ejecutar diariamente a las 2:00 AM
    @Transactional
    public void processExpiredMemberships() {
        logger.info("Iniciando proceso de verificación de membresías vencidas...");
        
        LocalDate today = LocalDate.now();
        
        // Buscar todas las suscripciones activas que han vencido
        List<Subscription> expiredSubscriptions = subscriptionRepository.findAll()
            .stream()
            .filter(subscription -> 
                subscription.getStatus() == Subscription.Status.ACTIVE &&
                subscription.getEndDate().isBefore(today)
            )
            .toList();

        logger.info("Encontradas {} membresías vencidas", expiredSubscriptions.size());

        int usersDeactivated = 0;
        int subscriptionsDeactivated = 0;

        for (Subscription subscription : expiredSubscriptions) {
            try {
                // Marcar la suscripción como expirada
                subscription.setStatus(Subscription.Status.EXPIRED);
                subscriptionRepository.save(subscription);
                subscriptionsDeactivated++;

                User user = subscription.getUser();
                
                // Solo desactivar usuarios MEMBER (no tocar OWNER/ADMIN)
                if (user.getRole() == User.UserRole.MEMBER && user.getStatus() == User.UserStatus.ACTIVE) {
                    user.setStatus(User.UserStatus.INACTIVE);
                    userRepository.save(user);
                    usersDeactivated++;
                    
                    logger.info("Usuario desactivado por membresía vencida: {} (ID: {})", 
                        user.getEmail(), user.getId());
                }

            } catch (Exception e) {
                logger.error("Error procesando suscripción vencida ID {}: {}", 
                    subscription.getId(), e.getMessage());
            }
        }

        logger.info("Proceso completado - Suscripciones desactivadas: {}, Usuarios desactivados: {}", 
            subscriptionsDeactivated, usersDeactivated);
    }

    /**
     * Método manual para forzar la verificación de membresías vencidas
     * (útil para testing o ejecución manual)
     */
    public void forceExpirationCheck() {
        logger.info("Ejecutando verificación manual de membresías vencidas...");
        processExpiredMemberships();
    }

    /**
     * Obtener estadísticas de membresías próximas a vencer
     */
    public String getMembershipExpirationStats() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        long expiredToday = subscriptionRepository.findAll()
            .stream()
            .filter(s -> s.getStatus() == Subscription.Status.ACTIVE && s.getEndDate().equals(today))
            .count();

        long expiringThisWeek = subscriptionRepository.findAll()
            .stream()
            .filter(s -> s.getStatus() == Subscription.Status.ACTIVE && 
                        s.getEndDate().isAfter(today) && 
                        s.getEndDate().isBefore(nextWeek))
            .count();

        return String.format("Membresías que vencen hoy: %d, Esta semana: %d", expiredToday, expiringThisWeek);
    }
}