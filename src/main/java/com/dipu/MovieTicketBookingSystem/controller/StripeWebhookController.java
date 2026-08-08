package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.service.BookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
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

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed.");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload.");
        }

        if (!AppConstants.EVENT_PAYMENT_INTENT_SUCCEEDED.equals(event.getType())) {
            log.warn("Unhandled event type: {}", event.getType());
            return ResponseEntity.ok("Success");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (dataObjectDeserializer.getObject().isEmpty()) {
            log.warn("Event data object is missing for event: {}", event.getId());
            return ResponseEntity.ok("Success");
        }

        StripeObject stripeObject = dataObjectDeserializer.getObject().get();
        if (!(stripeObject instanceof PaymentIntent)) {
            log.warn("Stripe object is not a PaymentIntent for event: {}", event.getId());
            return ResponseEntity.ok("Success");
        }

        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        handlePaymentIntentSucceeded(paymentIntent);

        return ResponseEntity.ok("Success");
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        try {
            // The booking ID is stored in the metadata during intent creation
            String bookingIdStr = paymentIntent.getMetadata().get("bookingId");
            if (bookingIdStr == null) {
                log.error("No bookingId found in payment intent metadata for intent: {}", paymentIntent.getId());
                return;
            }
            
            UUID bookingId = UUID.fromString(bookingIdStr);
            log.info("Payment succeeded for booking {}. Confirming booking...", bookingId);
            bookingService.confirmBooking(bookingId);
        } catch (Exception e) {
            log.error("Error processing successful payment intent: {}", e.getMessage(), e);
        }
    }
}
