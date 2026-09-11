package com.dipu.MovieTicketBookingSystem.service.payment;

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
import com.dipu.MovieTicketBookingSystem.exception.InvalidOperationException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service("stripe")
@Slf4j
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentIntentResponse createPaymentIntent(Booking booking) throws Exception {
        long amountInCents = booking.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        log.info("Creating Stripe PaymentIntent via LIGHTWEIGHT REST call for Booking ID: {} (Amount: {} paise)", booking.getId(), amountInCents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(stripeApiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("amount", String.valueOf(amountInCents));
        body.add("currency", "inr");
        body.add("metadata[bookingId]", booking.getId().toString());
        body.add("automatic_payment_methods[enabled]", "true");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.stripe.com/v1/payment_intents",
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String clientSecret = root.path("client_secret").asText();
                log.info("Successfully created Stripe PaymentIntent (Lightweight)");
                return new PaymentIntentResponse(clientSecret);
            } else {
                log.error("Failed to create PaymentIntent: {}", response.getBody());
                throw new InvalidOperationException("Failed to initialize payment gateway.");
            }
        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Stripe API responded with HTTP {}: {}", e.getStatusCode(), errorBody);

            String userMessage = "Payment gateway error. Please try again.";
            try {
                JsonNode errorJson = objectMapper.readTree(errorBody);
                String stripeCode = errorJson.path("error").path("code").asText("");
                String stripeMsg = errorJson.path("error").path("message").asText("");

                if ("amount_too_small".equalsIgnoreCase(stripeCode)) {
                    userMessage = "The total amount is below the online payment minimum (₹50). Please select additional seats or a different showtime.";
                } else if (!stripeMsg.isBlank()) {
                    userMessage = stripeMsg;
                }
            } catch (Exception parseEx) {
                log.warn("Could not parse Stripe error JSON: {}", parseEx.getMessage());
            }

            throw new InvalidOperationException(userMessage);
        }
    }
}
