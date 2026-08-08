package com.dipu.MovieTicketBookingSystem.repository;

import com.dipu.MovieTicketBookingSystem.model.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    
    // Find all future active showtimes for a specific movie
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    List<Showtime> findByMovieIdAndStartTimeAfterAndIsActiveTrueOrderByStartTimeAsc(UUID movieId, LocalDateTime now);

    // Find all future showtimes for a specific theater
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"movie", "screen", "screen.theater"})
    @Query("SELECT s FROM Showtime s WHERE s.screen.theater.id = :theaterId AND s.startTime > :now AND s.isActive = true ORDER BY s.startTime ASC")
    List<Showtime> findFutureShowtimesByTheater(@Param("theaterId") UUID theaterId, @Param("now") LocalDateTime now);
    
    // Find showtimes for a specific screen that overlap with a given time range
    @Query("SELECT s FROM Showtime s WHERE s.screen.id = :screenId AND s.isActive = true AND " +
           "(s.startTime < :endTime AND s.endTime > :startTime)")
    List<Showtime> findOverlappingShowtimes(@Param("screenId") UUID screenId, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
}
