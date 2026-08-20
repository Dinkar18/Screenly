package com.dipu.MovieTicketBookingSystem.service.payment;
import com.dipu.MovieTicketBookingSystem.service.BookingService;
import com.dipu.MovieTicketBookingSystem.dto.PaymentIntentResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Booking;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service("stripe")
@Slf4j
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final BookingService bookingService;

    @Override
    public PaymentIntentResponse createPaymentIntent(Booking booking) throws Exception {
        long amountInCents = booking.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        log.info("Creating Stripe PaymentIntent via LIGHTWEIGHT REST call for Booking ID: {}", booking.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(stripeApiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("amount", String.valueOf(amountInCents));
        body.add("currency", "inr");
        body.add("metadata[bookingId]", booking.getId().toString());
        body.add("automatic_payment_methods[enabled]", "true");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.stripe.com/v1/payment_intents",
                request,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String clientSecret = root.path("client_secret").asText();
            
            // This bypasses the need for the Stripe Webhook which isn't configured in the Render environment!
            try {
                bookingService.confirmBooking(booking.getId());
                log.info("Automatically confirmed booking {} for demo bypass", booking.getId());
            } catch (Exception e) {
                log.error("Failed to auto-confirm booking: {}", e.getMessage());
            }
            
            log.info("Successfully created Stripe PaymentIntent (Lightweight)");
            return new PaymentIntentResponse(clientSecret);
        } else {
            log.error("Failed to create PaymentIntent: {}", response.getBody());
            throw new RuntimeException("Stripe API error: " + response.getBody());
        }
    }
}
