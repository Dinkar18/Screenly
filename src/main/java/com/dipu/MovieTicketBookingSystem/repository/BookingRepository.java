package com.dipu.MovieTicketBookingSystem.repository;

import com.dipu.MovieTicketBookingSystem.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    // For idempotency check
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    // Get booking history for a user
    org.springframework.data.domain.Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, org.springframework.data.domain.Pageable pageable);

    // Get ONLY confirmed bookings for the frontend dashboard
    org.springframework.data.domain.Page<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus status, org.springframework.data.domain.Pageable pageable);
}
