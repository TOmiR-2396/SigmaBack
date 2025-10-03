package com.example.gym.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    // Tu API key de Resend (mejor ponerla en application.yml)
    @Value("${resend.api.key:re_TPpcUdx1_3SE1iat5PUgvL3VuAxLkDfL1}")
    private String resendApiKey;
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            Resend resend = new Resend(resendApiKey);
            
            // Crear URL de reset con el token - apuntando al frontend
            String resetUrl = "http://localhost:5173/reset-password?token=" + resetToken;
            
            // Crear HTML del email
            String htmlContent = buildPasswordResetHtml(resetUrl, toEmail);
            
            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .from("noreply@resend.dev") // Cambia por tu dominio cuando tengas uno
                    .to(toEmail) // Email del usuario que solicita el cambio
                    .subject("Recuperar Contraseña - Gym App")
                    .html(htmlContent)
                    .build();
            
            SendEmailResponse response = resend.emails().send(sendEmailRequest);
            
            System.out.println("Email enviado exitosamente. ID: " + response.getId());
            
        } catch (ResendException e) {
            System.err.println("Error de Resend: " + e.getMessage());
            throw new RuntimeException("Error al enviar email de recuperación", e);
        } catch (Exception e) {
            System.err.println("Error general enviando email: " + e.getMessage());
            throw new RuntimeException("Error al enviar email de recuperación", e);
        }
    }
    
    private String buildPasswordResetHtml(String resetUrl, String userEmail) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset=\"UTF-8\">" +
               "<title>Recuperar Contraseña</title>" +
               "<style>" +
               "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
               ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
               ".header { background: #007bff; color: white; padding: 20px; text-align: center; }" +
               ".content { padding: 20px; background: #f9f9f9; }" +
               ".button { display: inline-block; padding: 12px 24px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
               ".footer { padding: 20px; font-size: 12px; color: #666; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class=\"container\">" +
               "<div class=\"header\">" +
               "<h1>🏋️ Gym App</h1>" +
               "<h2>Recuperar Contraseña</h2>" +
               "</div>" +
               "<div class=\"content\">" +
               "<p>Hola,</p>" +
               "<p>Recibimos una solicitud para cambiar la contraseña de tu cuenta: <strong>" + userEmail + "</strong></p>" +
               "<p>Si fuiste tú quien solicitó este cambio, haz clic en el siguiente botón:</p>" +
               "<div style=\"text-align: center;\">" +
               "<a href=\"" + resetUrl + "\" class=\"button\">🔒 Cambiar Mi Contraseña</a>" +
               "</div>" +
               "<p><strong>Importante:</strong></p>" +
               "<ul>" +
               "<li>Este enlace expira en <strong>1 hora</strong></li>" +
               "<li>Solo se puede usar <strong>una vez</strong></li>" +
               "<li>Si no solicitaste este cambio, ignora este email</li>" +
               "</ul>" +
               "<p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>" +
               "<p style=\"word-break: break-all; color: #007bff;\">" + resetUrl + "</p>" +
               "</div>" +
               "<div class=\"footer\">" +
               "<p>Este email fue enviado automáticamente. No respondas a este mensaje.</p>" +
               "<p>© 2024 Gym App - Todos los derechos reservados</p>" +
               "</div>" +
               "</div>" +
               "</body>" +
               "</html>";
    }
}