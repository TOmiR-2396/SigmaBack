package com.example.gym.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resuelve el tenant actual y habilita el filtro de Hibernate por request.
 */
@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    @Value("${multitenancy.header:X-Tenant-ID}")
    private String headerName;

    @Value("${multitenancy.default-tenant:}")
    private String defaultTenant;

    @Value("${multitenancy.required:true}")
    private boolean requireTenant;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = resolveTenant(request);

        if (requireTenant && !StringUtils.hasText(tenantId)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("{\"error\":\"tenant_required\"}");
            return;
        }

        try {
            if (StringUtils.hasText(tenantId)) {
                TenantContext.setCurrentTenant(tenantId);
                enableTenantFilter(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            disableTenantFilter();
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        String value = request.getHeader(headerName);
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        if (StringUtils.hasText(defaultTenant)) {
            return defaultTenant.trim();
        }
        return null;
    }

    private void enableTenantFilter(String tenantId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } catch (Exception ignored) {
            // Si no hay EntityManager aún, el filter no se aplica
        }
    }

    private void disableTenantFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") != null) {
                session.disableFilter("tenantFilter");
            }
        } catch (Exception ignored) {
        }
    }
}
