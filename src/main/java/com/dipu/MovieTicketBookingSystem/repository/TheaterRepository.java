package com.dipu.MovieTicketBookingSystem.repository;

import com.dipu.MovieTicketBookingSystem.model.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, UUID> {
    List<Theater> findByIsActiveTrue();
    List<Theater> findByCityIgnoreCaseAndIsActiveTrue(String city);
}
