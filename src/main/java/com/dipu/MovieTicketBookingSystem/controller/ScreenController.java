package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.dto.ScreenRequest;
import com.dipu.MovieTicketBookingSystem.dto.ScreenResponse;
import com.dipu.MovieTicketBookingSystem.service.TheaterService;
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
@RequestMapping("/api/v1/screens")
@RequiredArgsConstructor
@Slf4j
public class ScreenController {

    private final TheaterService theaterService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScreenResponse> addScreen(@Valid @RequestBody ScreenRequest request) {
        log.info("Adding screen to theater: {}", request.getTheaterId());
        return new ResponseEntity<>(theaterService.addScreen(request), HttpStatus.CREATED);
    }

    @GetMapping("/theater/{theaterId}")
    public ResponseEntity<List<ScreenResponse>> getScreensByTheaterId(@PathVariable UUID theaterId) {
        return ResponseEntity.ok(theaterService.getScreensByTheaterId(theaterId));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScreenResponse> updateScreen(@PathVariable UUID id, @Valid @RequestBody ScreenRequest request) {
        log.info("Updating screen with id: {}", id);
        return ResponseEntity.ok(theaterService.updateScreen(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteScreen(@PathVariable UUID id) {
        log.info("Deleting screen with id: {}", id);
        theaterService.deleteScreen(id);
        return ResponseEntity.noContent().build();
    }
}
