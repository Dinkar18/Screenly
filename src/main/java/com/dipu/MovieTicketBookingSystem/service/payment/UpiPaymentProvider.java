package com.dipu.MovieTicketBookingSystem.service.payment;

import com.dipu.MovieTicketBookingSystem.dto.PaymentIntentResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("upi")
@Slf4j
public class UpiPaymentProvider implements PaymentProvider {

    @Override
    public PaymentIntentResponse createPaymentIntent(Booking booking) throws Exception {
        log.info("Creating UPI Payment request for Booking ID: {}, Amount: {}", booking.getId(), booking.getTotalAmount());
        
        // Mocking a UPI deep link or transaction ID creation
        String upiTransactionId = "upi_tx_" + UUID.randomUUID().toString();
        
        log.info("Successfully created UPI transaction: {}", upiTransactionId);

        // Returning the transaction ID. In a real scenario, this might be a UPI intent URI.
        return new PaymentIntentResponse(upiTransactionId);
    }
}
