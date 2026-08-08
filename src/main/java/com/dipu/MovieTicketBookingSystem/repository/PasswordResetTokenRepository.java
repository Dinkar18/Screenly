package com.dipu.MovieTicketBookingSystem.repository;

import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
}
