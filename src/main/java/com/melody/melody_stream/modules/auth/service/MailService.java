package com.melody.melody_stream.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends verification email asynchronously — does not block auth flow.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        String verifyUrl = "%s/api/auth/verify-email?token=%s".formatted(baseUrl, token);
        String html = """
            <h2>Welcome to Melody, %s!</h2>
            <p>Please verify your email by clicking the button below:</p>
            <a href="%s" style="
                background:#6366f1;
                color:white;
                padding:12px 24px;
                border-radius:6px;
                text-decoration:none;
                display:inline-block;
            ">Verify Email</a>
            <p>This link expires in 24 hours.</p>
            <p>If you didn't register, you can safely ignore this email.</p>
        """.formatted(username, verifyUrl);

        try {
            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your Melody account");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            // Never fail registration because of mail issues
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }
}
