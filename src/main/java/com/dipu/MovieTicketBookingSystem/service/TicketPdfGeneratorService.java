package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class TicketPdfGeneratorService {

    public byte[] generateTicketPdf(BookingResponse booking) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        // Header
        Paragraph header = new Paragraph("CINE RESERVE - MOVIE TICKET", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(20);
        document.add(header);

        // Movie Info
        document.add(new Paragraph("Movie: " + booking.getMovieTitle(), subtitleFont));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        document.add(new Paragraph("Showtime: " + booking.getShowtime().format(formatter), regularFont));
        
        document.add(new Paragraph("Theater: " + booking.getTheaterName(), regularFont));
        document.add(new Paragraph("Screen: " + booking.getScreenName(), regularFont));
        
        document.add(new Paragraph("\n"));

        // Seats & Amount
        document.add(new Paragraph("Seats: " + String.join(", ", booking.getBookedSeats()), subtitleFont));
        document.add(new Paragraph("Total Amount: INR " + booking.getTotalAmount(), regularFont));
        document.add(new Paragraph("Booking ID: " + booking.getId(), regularFont));

        document.add(new Paragraph("\n\n"));
        Paragraph footer = new Paragraph("Please present this ticket at the entrance.", regularFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }
}
