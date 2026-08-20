package com.dipu.MovieTicketBookingSystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            System.out.println("========== EMAIL INTERCEPT (REST API) ==========");
            System.out.println("TO: " + to);
            System.out.println("SUBJECT: " + subject);
            System.out.println("================================================");
            
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("email", fromEmail, "name", "CineReserve"));
            body.put("to", List.of(Map.of("email", to)));
            body.put("subject", subject);
            body.put("htmlContent", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            System.out.println("Brevo API Response: " + response.getStatusCode());
            
        } catch (Exception e) {
            System.err.println("FAILED TO SEND EMAIL TO " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
