package com.example.gym.service;

import com.example.gym.tenant.TenantSwitch;
import com.example.gym.tenant.TenantSwitchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserInactivityService {

    private static final Logger logger = LoggerFactory.getLogger(UserInactivityService.class);
    private static final String SWITCH_KEY = "INACTIVITY_AUTO_DEACTIVATE";
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)");

    private final TenantSwitchRepository tenantSwitchRepository;
    private final UserDeactivationService userDeactivationService;

    @Value("${user.inactivity.default-days:14}")
    private int defaultInactivityDays;

    public UserInactivityService(TenantSwitchRepository tenantSwitchRepository,
                                 UserDeactivationService userDeactivationService) {
        this.tenantSwitchRepository = tenantSwitchRepository;
        this.userDeactivationService = userDeactivationService;
    }

    @Scheduled(cron = "0 30 2 * * *") // 02:30 AM diario
    @Transactional
    public void deactivateInactiveUsers() {
        List<TenantSwitch> switches = tenantSwitchRepository.findByKey(SWITCH_KEY);
        if (switches.isEmpty()) {
            return;
        }

        for (TenantSwitch sw : switches) {
            if (!sw.isEnabled()) {
                continue;
            }
            String tenantId = sw.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                continue;
            }
            int days = parseDays(sw.getPayload());
            if (days < 1) {
                days = Math.max(defaultInactivityDays, 1);
            }
            LocalDate cutoffDate = LocalDate.now().minusDays(days);
            int deactivated = userDeactivationService.deactivateInactiveMembers(
                tenantId,
                cutoffDate,
                "INACTIVITY"
            );
            if (deactivated > 0) {
                logger.info("Tenant {}: desactivados {} usuarios por inactividad ({} días)", tenantId, deactivated, days);
            }
        }
    }

    private int parseDays(String payload) {
        if (payload == null || payload.isBlank()) {
            return defaultInactivityDays;
        }
        try {
            return Integer.parseInt(payload.trim());
        } catch (NumberFormatException ignored) {
        }
        Matcher matcher = DAYS_PATTERN.matcher(payload);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return defaultInactivityDays;
            }
        }
        return defaultInactivityDays;
    }
}
