package com.braincache.service;

import com.braincache.config.ReviewProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over Spring's JavaMailSender (auto-configured from spring.mail.*).
 * One job: send a plain-text email. Keeps the SMTP mechanics out of the scheduler.
 *
 * Go parallel: a small struct holding an *smtp.Client with a Send(to, subject, body).
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, ReviewProperties review) {
        this.mailSender = mailSender;
        this.from = review.emailFrom();
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
