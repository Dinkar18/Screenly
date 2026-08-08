package com.dipu.MovieTicketBookingSystem.dto;

import com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID userId;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private LocalDateTime showtime;
    private List<String> bookedSeats;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
