package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.dto.PaymentIntentResponse;
import com.dipu.MovieTicketBookingSystem.dto.MessageResponse;
import com.dipu.MovieTicketBookingSystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final com.dipu.MovieTicketBookingSystem.service.BookingService bookingService;

    @PostMapping("/create-intent/{bookingId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "stripe") String provider) throws Exception {
            
        log.info("Request received to create payment intent for booking ID: {} via provider: {}", bookingId, provider);
        
        PaymentIntentResponse response = paymentService.createPaymentIntent(bookingId, provider);
        
        return ResponseEntity.ok(response);
    }

}
