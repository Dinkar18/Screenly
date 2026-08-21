package com.dipu.MovieTicketBookingSystem.dto;

import com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private UUID showtimeSeatId;
    private String seatIdentifier; // e.g. "A1" (Legacy)
    private int rowNumber;         // Clean standard
    private String seatLetter;     // Clean standard
    private SeatStatus status;
}
