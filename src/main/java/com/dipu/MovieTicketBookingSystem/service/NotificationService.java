package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import lombok.RequiredArgsConstructor;
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

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TicketPdfGeneratorService ticketPdfGeneratorService;
    private final RestTemplate restTemplate;

    @Value("${brevo.api.key:}")
    private String apiKey;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Async
    public void sendBookingConfirmation(BookingResponse bookingResponse, String userEmail) {
        log.info("Starting background task to generate PDF and send email to {}", userEmail);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("NOTIFICATION EMAIL SKIPPED: 'BREVO_API_KEY' is not configured.");
            return;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("NOTIFICATION EMAIL SKIPPED: 'MAIL_USERNAME' is not configured.");
            return;
        }

        try {
            // 1. Generate PDF Ticket
            byte[] pdfBytes = ticketPdfGeneratorService.generateTicketPdf(bookingResponse);
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            // 2. Send Email with Attachment using Brevo REST API
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey.trim());
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("email", fromEmail.trim(), "name", "Screenly"));
            body.put("to", List.of(Map.of("email", userEmail.trim())));
            body.put("subject", "Your Movie Ticket - " + bookingResponse.getMovieTitle());
            body.put("textContent", "Hi there,\n\nYour booking is confirmed! Please find your PDF ticket attached to this email.\n\nPlease arrive 15 minutes early.\n\nEnjoy the movie!");
            
            // Attach PDF
            body.put("attachment", List.of(Map.of(
                    "name", "MovieTicket_" + bookingResponse.getId() + ".pdf",
                    "content", base64Pdf
            )));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Successfully sent PDF ticket email via Brevo REST API to {} with status {}", userEmail, response.getStatusCode());

        } catch (com.itextpdf.text.DocumentException e) {
            log.error("Failed to generate PDF for booking {}: {}", bookingResponse.getId(), e.getMessage());
        } catch (RestClientResponseException e) {
            log.error("Brevo API Rejected Ticket Email to {}: HTTP {} - Response Body: {}", 
                    userEmail, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to send email to {} for booking {}: {}", userEmail, bookingResponse.getId(), e.getMessage(), e);
        }
    }
}
