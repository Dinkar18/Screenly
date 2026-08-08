package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private NotificationService notificationService;

    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingResponse = BookingResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .movieTitle("The Matrix")
                .theaterName("AMC Test")
                .screenName("Screen 1")
                .showtime(LocalDateTime.now().plusDays(1))
                .bookedSeats(List.of("A1", "A2"))
                .totalAmount(BigDecimal.valueOf(300.00))
                .status(BookingStatus.CONFIRMED)
                .build();
    }

    @Test
    void sendBookingConfirmation_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.sendBookingConfirmation(bookingResponse, "test@cinereserve.com");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendBookingConfirmation_ExceptionThrown_DoesNotPropagate() {
        // If createMimeMessage throws an exception, it should be caught and logged
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server down"));

        // Call the method, expecting it not to throw (since it's wrapped in a try/catch)
        notificationService.sendBookingConfirmation(bookingResponse, "test@cinereserve.com");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
