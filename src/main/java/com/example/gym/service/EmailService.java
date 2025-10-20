package com.example.gym.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${mail.from:noreply@sigmagym.com.ar}")
    private String mailFrom;

    @Value("${app.frontend.reset-url:http://localhost:5173/reset-password?token=}")
    private String resetBaseUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            // Construir URL al frontend
            String resetUrl = resetBaseUrl + resetToken;

            // Crear mensaje HTML
            String htmlContent = buildPasswordResetHtml(resetUrl, toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(mailFrom));
            helper.setTo(toEmail);
            helper.setSubject("Recuperar Contraseña - Sigma Gym");
            helper.setText(htmlContent, true); // HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error creando el email de recuperación", e);
        } catch (Exception e) {
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