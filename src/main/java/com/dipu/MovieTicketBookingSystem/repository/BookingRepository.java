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

    // Check if any confirmed bookings exist for future showtimes at a specific theater
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(b) > 0 FROM Booking b " +
        "WHERE b.showtime.screen.theater.id = :theaterId " +
        "AND b.showtime.startTime > :now " +
        "AND b.status = com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus.CONFIRMED"
    )
    boolean existsActiveFutureBookingsByTheater(@org.springframework.data.repository.query.Param("theaterId") UUID theaterId, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    // Check if any confirmed bookings exist for future showtimes on a specific screen
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(b) > 0 FROM Booking b " +
        "WHERE b.showtime.screen.id = :screenId " +
        "AND b.showtime.startTime > :now " +
        "AND b.status = com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus.CONFIRMED"
    )
    boolean existsActiveFutureBookingsByScreen(@org.springframework.data.repository.query.Param("screenId") UUID screenId, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
