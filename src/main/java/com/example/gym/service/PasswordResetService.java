package com.example.gym.service;

import com.example.gym.model.PasswordResetToken;
import com.example.gym.model.User;
import com.example.gym.repository.PasswordResetTokenRepository;
import com.example.gym.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Generar token de recuperación
    @Transactional
    public String createPasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        User user = userOpt.get();
        
        // Invalidar tokens anteriores del usuario
        tokenRepository.invalidateAllUserTokens(user);
        
        // Generar nuevo token
        String token = UUID.randomUUID().toString();
        
        // Crear token con expiración de 1 hora
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        
        tokenRepository.save(resetToken);
        
        return token;
    }
    
        // Validar token de reset
    @Transactional
    public boolean validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenWithUser(token);
        return tokenOpt.isPresent() && tokenOpt.get().isValid();
    }
    
        // Resetear contraseña
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenWithUser(token);
        
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            return false;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        
        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Marcar token como usado
        tokenRepository.delete(resetToken);
        
        return true;
    }
    
    // Obtener usuario por token
    @Transactional
    public Optional<User> getUserByToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenWithUser(token);
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            return Optional.empty();
        }
        
        return Optional.of(tokenOpt.get().getUser());
    }
}