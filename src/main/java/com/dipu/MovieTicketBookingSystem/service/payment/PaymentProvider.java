package com.dipu.MovieTicketBookingSystem.service.payment;

import com.dipu.MovieTicketBookingSystem.dto.PaymentIntentResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Booking;

public interface PaymentProvider {
    /**
     * Creates a payment intent or transaction for the given booking.
     *
     * @param booking The booking details
     * @return PaymentIntentResponse containing the client secret or transaction ID
     * @throws Exception if payment creation fails
     */
    PaymentIntentResponse createPaymentIntent(Booking booking) throws Exception;
}
