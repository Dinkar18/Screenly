package com.dipu.MovieTicketBookingSystem.repository;

import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);

    @Transactional
    void deleteByUser(User user);
}
