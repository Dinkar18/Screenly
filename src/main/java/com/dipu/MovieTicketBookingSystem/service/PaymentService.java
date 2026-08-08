package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.model.entity.Booking;
import com.dipu.MovieTicketBookingSystem.repository.BookingRepository;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.exception.InvalidOperationException;
import com.dipu.MovieTicketBookingSystem.dto.PaymentIntentResponse;
import com.dipu.MovieTicketBookingSystem.service.payment.PaymentProvider;
import com.dipu.MovieTicketBookingSystem.service.payment.PaymentProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentProviderFactory providerFactory;

    public PaymentIntentResponse createPaymentIntent(java.util.UUID bookingId, String paymentMethod) throws Exception {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() == com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus.CONFIRMED) {
            throw new InvalidOperationException("Booking is already confirmed");
        }

        // 1. Get the correct provider dynamically (defaults to Stripe if null)
        PaymentProvider paymentProvider = providerFactory.getProvider(paymentMethod);

        // 2. Delegate intent creation to the provider
        return paymentProvider.createPaymentIntent(booking);
    }
}
