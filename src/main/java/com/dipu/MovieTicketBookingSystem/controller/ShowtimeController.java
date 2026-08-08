package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.dto.ShowtimeRequest;
import com.dipu.MovieTicketBookingSystem.dto.ShowtimeResponse;
import com.dipu.MovieTicketBookingSystem.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
@Slf4j
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowtimeResponse> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        log.info("Creating showtime for movie: {} on screen: {}", request.getMovieId(), request.getScreenId());
        return new ResponseEntity<>(showtimeService.createShowtime(request), HttpStatus.CREATED);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(showtimeService.getShowtimesByMovie(movieId));
    }

    @GetMapping("/theater/{theaterId}")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByTheater(@PathVariable UUID theaterId) {
        return ResponseEntity.ok(showtimeService.getShowtimesByTheater(theaterId));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowtimeResponse> updateShowtime(@PathVariable UUID id, @Valid @RequestBody ShowtimeRequest request) {
        log.info("Updating showtime with id: {}", id);
        return ResponseEntity.ok(showtimeService.updateShowtime(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShowtime(@PathVariable UUID id) {
        log.info("Deleting showtime with id: {}", id);
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}
