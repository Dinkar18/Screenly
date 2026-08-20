package com.dipu.MovieTicketBookingSystem.repository;

import com.dipu.MovieTicketBookingSystem.model.entity.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, UUID> {
    
    // Fix N+1 Query Problem: Use JOIN FETCH to grab the underlying Seat entity in 1 query instead of 200 queries
    @Query("SELECT s FROM ShowtimeSeat s JOIN FETCH s.seat WHERE s.showtime.id = :showtimeId")
    List<ShowtimeSeat> findByShowtimeId(@Param("showtimeId") UUID showtimeId);

    // CRITICAL: Pessimistic locking to prevent double bookings
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints({
        @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") // 3 seconds timeout
    })
    @Query("SELECT s FROM ShowtimeSeat s WHERE s.id IN :ids")
    List<ShowtimeSeat> findByIdsForUpdate(@Param("ids") List<UUID> ids);
}
