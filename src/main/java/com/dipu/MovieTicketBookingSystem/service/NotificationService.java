package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final TicketPdfGeneratorService ticketPdfGeneratorService;

    @Value("${mail.from.address:noreply@cinereserve.com}")
    private String fromAddress;

    @Async
    public void sendBookingConfirmation(BookingResponse bookingResponse, String userEmail) {
        log.info("Starting background task to generate PDF and send email to {}", userEmail);

        try {
            // 1. Generate PDF Ticket
            byte[] pdfBytes = ticketPdfGeneratorService.generateTicketPdf(bookingResponse);

            // 2. Send Email with Attachment
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(userEmail);
            helper.setSubject("Your Movie Ticket - " + bookingResponse.getMovieTitle());
            helper.setText("Hi there,\n\nYour booking is confirmed! Please find your PDF ticket attached to this email.\n\nPlease arrive 15 minutes early.\n\nEnjoy the movie!");
            helper.setFrom(fromAddress);

            // Attach PDF
            helper.addAttachment("MovieTicket_" + bookingResponse.getId() + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Successfully sent PDF ticket email to {}", userEmail);

        } catch (com.itextpdf.text.DocumentException e) {
            log.error("Failed to generate PDF for booking {}: {}", bookingResponse.getId(), e.getMessage());
        } catch (MessagingException e) {
            log.error("Failed to send email to {} for booking {}: {}", userEmail, bookingResponse.getId(), e.getMessage());
        }
    }
}
