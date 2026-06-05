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

    public void sendContactEmail(String toEmail, String gymName, String subject, String message) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(new InternetAddress(mailFrom));
            helper.setTo(toEmail);
            helper.setSubject("[GestiGym] " + subject);
            String html = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>"
                + "<div style='background:#14213D;padding:20px;text-align:center;'>"
                + "<h2 style='color:#fff;margin:0;'>GESTIGYM</h2></div>"
                + "<div style='padding:24px;'>"
                + "<p>Hola <strong>" + gymName + "</strong>,</p>"
                + "<p>" + message.replace("\n", "<br>") + "</p>"
                + "</div>"
                + "<div style='background:#f4f4f4;padding:16px;text-align:center;font-size:12px;color:#666;'>"
                + "GestiGym — Plataforma de gestión para gimnasios</div></div>";
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception ignored) {}
    }

    public void sendPaymentReceipt(String toEmail, String memberName,
                                   String planName, double amount,
                                   String method, String startDate, String endDate,
                                   String mpPaymentId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(mailFrom));
            helper.setTo(toEmail);
            helper.setSubject("Comprobante de pago - Sigma Gym");
            helper.setText(buildReceiptHtml(memberName, planName, amount, method,
                                            startDate, endDate, mpPaymentId), true);
            mailSender.send(message);
        } catch (Exception e) {
            // No interrumpir el flujo de pago si el mail falla
        }
    }

    private String buildReceiptHtml(String memberName, String planName, double amount,
                                    String method, String startDate, String endDate,
                                    String mpPaymentId) {
        String amountStr = String.format("$ %,.2f ARS", amount).replace(",", "X").replace(".", ",").replace("X", ".");
        String methodLabel = "MP".equals(method) ? "Mercado Pago" : "Efectivo";
        String paymentRow = mpPaymentId != null
            ? "<tr><td style='padding:8px 0;color:#666;'>N° de pago</td><td style='padding:8px 0;text-align:right;font-weight:600;'>" + mpPaymentId + "</td></tr>"
            : "";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;'>" +
            "<div style='max-width:560px;margin:32px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>" +
            // Header
            "<div style='background:#14213D;padding:32px 32px 24px;text-align:center;'>" +
            "<h1 style='color:#fff;margin:0;font-size:24px;letter-spacing:1px;'>SIGMA GYM</h1>" +
            "<p style='color:#9AB8E4;margin:6px 0 0;font-size:13px;'>Comprobante de pago</p>" +
            "</div>" +
            // Body
            "<div style='padding:32px;'>" +
            "<p style='color:#333;font-size:15px;margin:0 0 24px;'>Hola <strong>" + memberName + "</strong>,<br>tu pago fue procesado exitosamente.</p>" +
            "<table style='width:100%;border-collapse:collapse;border-top:1px solid #eee;'>" +
            "<tr><td style='padding:12px 0;color:#666;'>Plan</td><td style='padding:12px 0;text-align:right;font-weight:700;color:#14213D;font-size:16px;'>" + planName + "</td></tr>" +
            "<tr style='background:#f9f9f9;'><td style='padding:8px 12px;color:#666;'>Vigencia</td><td style='padding:8px 12px;text-align:right;font-weight:600;'>" + startDate + " → " + endDate + "</td></tr>" +
            "<tr><td style='padding:8px 0;color:#666;'>Método de pago</td><td style='padding:8px 0;text-align:right;font-weight:600;'>" + methodLabel + "</td></tr>" +
            paymentRow +
            "<tr style='border-top:2px solid #14213D;'><td style='padding:16px 0;font-size:17px;font-weight:700;color:#14213D;'>Total abonado</td>" +
            "<td style='padding:16px 0;text-align:right;font-size:22px;font-weight:800;color:#14213D;'>" + amountStr + "</td></tr>" +
            "</table>" +
            "<div style='background:#f0f9f4;border:1px solid #b6e8c8;border-radius:8px;padding:14px 18px;margin-top:24px;'>" +
            "<p style='margin:0;color:#1a7a40;font-size:13px;'>Tu membresía está activa. Podés reservar tus turnos desde la app.</p>" +
            "</div>" +
            "</div>" +
            // Footer
            "<div style='background:#f9f9f9;padding:20px 32px;text-align:center;border-top:1px solid #eee;'>" +
            "<p style='margin:0;font-size:12px;color:#999;'>Este es un comprobante automático. No respondas este email.<br>© 2026 Sigma Gym</p>" +
            "</div>" +
            "</div></body></html>";
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