package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.dto.BookingRequest;
import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import com.dipu.MovieTicketBookingSystem.dto.SeatResponse;
import com.dipu.MovieTicketBookingSystem.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/showtimes/{showtimeId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeatsForShowtime(@PathVariable UUID showtimeId) {
        return ResponseEntity.ok(bookingService.getSeatsForShowtime(showtimeId));
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        
        String userEmail = authentication.getName();

        BookingResponse response = bookingService.createBooking(request, userEmail, idempotencyKey);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("id", response.getId().toString()));
    }

    @GetMapping("/bookings/my-bookings")
    public ResponseEntity<com.dipu.MovieTicketBookingSystem.dto.PageResponse<BookingResponse>> getMyBookings(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userEmail = authentication.getName();
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.getMyBookings(userEmail, pageRequest));
    }
}
