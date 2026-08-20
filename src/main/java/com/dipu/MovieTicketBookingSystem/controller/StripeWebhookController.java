package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.service.BookingService;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            verifySignature(payload, sigHeader);
            processWebhookPayload(payload);
            return ResponseEntity.ok("Success");
            
        } catch (SignatureVerificationException e) {
            log.error("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed.");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.ok("Invalid payload.");
        }
    }

    private void verifySignature(String payload, String sigHeader) throws SignatureVerificationException {
        Webhook.Signature.verifyHeader(payload, sigHeader, endpointSecret, 300);
    }

    private void processWebhookPayload(String payload) throws Exception {
        JsonNode rootNode = objectMapper.readTree(payload);
        String eventType = rootNode.path("type").asText();

        if (!AppConstants.EVENT_PAYMENT_INTENT_SUCCEEDED.equals(eventType)) {
            log.debug("Ignoring unhandled event type: {}", eventType);
            return;
        }

        JsonNode metadataNode = rootNode.path("data").path("object").path("metadata");
        String bookingIdStr = metadataNode.path("bookingId").asText(null);

        if (bookingIdStr == null || bookingIdStr.isEmpty()) {
            log.warn("No bookingId found in payment intent metadata. Skipping processing.");
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);
        log.info("Payment intent succeeded. Confirming booking ID: {}", bookingId);
        bookingService.confirmBooking(bookingId);
    }
}
