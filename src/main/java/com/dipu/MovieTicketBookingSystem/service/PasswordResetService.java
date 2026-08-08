package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.ForgotPasswordRequest;
import com.dipu.MovieTicketBookingSystem.dto.ResetPasswordRequest;
import com.dipu.MovieTicketBookingSystem.model.entity.PasswordResetToken;
import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.repository.PasswordResetTokenRepository;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.exception.InvalidTokenException;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public String processForgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with this email"));

        String otp = String.format("%06d", new Random().nextInt(999999));

        // Delete any existing token
        passwordResetTokenRepository.findByUser(user)
                .ifPresent(passwordResetTokenRepository::delete);

        PasswordResetToken token = PasswordResetToken.builder()
                .token(otp)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES))
                .build();

        passwordResetTokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                AppConstants.RESET_EMAIL_SUBJECT,
                "Your password reset OTP is: " + otp + "\nIt will expire in " + AppConstants.OTP_EXPIRY_MINUTES + " minutes."
        );

        return "Password reset OTP sent to email";
    }

    public String processResetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or missing reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        return "Password has been successfully reset. You can now login.";
    }
}
