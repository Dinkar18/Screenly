package com.dipu.MovieTicketBookingSystem.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Showtime ID is required")
    private UUID showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<UUID> showtimeSeatIds;
}
