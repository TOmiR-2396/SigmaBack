package com.example.gym.security;

import com.example.gym.model.User;
import com.example.gym.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        
        // Endpoints públicos que no requieren autenticación
        if (path.equals("/api/auth/register") || 
            path.equals("/api/auth/login") ||
            path.equals("/api/auth/forgot-password") ||
            path.equals("/api/auth/reset-password") ||
            path.equals("/api/auth/validate-reset-token")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Para todos los demás endpoints, verificar JWT
        String header = request.getHeader("Authorization");
        String token = null;
        String email = null;
        
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            try {
                email = jwtUtil.getEmailFromToken(token);
            } catch (Exception e) {
                // Token inválido o expirado - no loguear detalles en producción
            }
        }
        
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && jwtUtil.validateToken(token)) {
                // Verificar que el usuario esté activo para endpoints protegidos
                if (user.getStatus() == User.UserStatus.ACTIVE || 
                    path.equals("/api/me")) { // /me permite ver perfil aunque esté inactivo
                    
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user, null, java.util.List.of(authority));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
