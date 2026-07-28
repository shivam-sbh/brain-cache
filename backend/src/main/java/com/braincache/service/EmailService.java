package com.braincache.service;

import com.braincache.config.ReviewProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over Spring's JavaMailSender (auto-configured from spring.mail.*).
 * One job: send an already-built HTML document. Callers build the full document via
 * EmailTemplate.wrap() themselves — this class just handles SMTP mechanics.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, ReviewProperties review) {
        this.mailSender = mailSender;
        this.from = review.emailFrom();
    }

    public void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to build email to " + to, e);
        }
    }
}
