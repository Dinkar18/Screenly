package com.dipu.MovieTicketBookingSystem.service.payment;

import com.dipu.MovieTicketBookingSystem.dto.PaymentIntentResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Booking;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("stripe")
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("StripePaymentProvider initialized with key: {}", stripeApiKey != null ? "HIDDEN" : "NULL");
    }

    @Override
    public PaymentIntentResponse createPaymentIntent(Booking booking) throws Exception {
        // Stripe expects amount in smallest currency unit (e.g., cents for USD/INR)
        long amountInCents = booking.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("inr")
                .putMetadata("bookingId", booking.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        log.info("Creating Stripe PaymentIntent for Booking ID: {}, Amount: {}", booking.getId(), booking.getTotalAmount());
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.info("Successfully created Stripe PaymentIntent: {}", paymentIntent.getId());

        return new PaymentIntentResponse(paymentIntent.getClientSecret());
    }
}
