package com.dipu.MovieTicketBookingSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${brevo.api.key:}")
    private String apiKey;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("BREVO EMAIL SKIPPED: 'BREVO_API_KEY' environment variable is missing or empty.");
            return;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("BREVO EMAIL SKIPPED: 'MAIL_USERNAME' (Sender Email) environment variable is missing or empty.");
            return;
        }

        try {
            log.info("Sending transactional email via Brevo REST API to: {} | Subject: {}", to, subject);
            
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey.trim());
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("email", fromEmail.trim(), "name", "Screenly"));
            body.put("to", List.of(Map.of("email", to.trim())));
            body.put("subject", subject);
            body.put("htmlContent", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Brevo Email successfully delivered for {} - Status: {}", to, response.getStatusCode());
            
        } catch (RestClientResponseException e) {
            log.error("Brevo API Rejected Email to {}: HTTP {} - Response Body: {}", 
                    to, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
        }
    }
}
