package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.AuthRequest;
import com.dipu.MovieTicketBookingSystem.dto.AuthResponse;
import com.dipu.MovieTicketBookingSystem.dto.RegisterRequest;
import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.enums.Role;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import com.dipu.MovieTicketBookingSystem.security.CustomUserDetails;
import com.dipu.MovieTicketBookingSystem.security.JwtUtil;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import com.dipu.MovieTicketBookingSystem.model.entity.VerificationToken;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.dipu.MovieTicketBookingSystem.exception.EmailAlreadyRegisteredException;
import com.dipu.MovieTicketBookingSystem.exception.InvalidTokenException;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.repository.VerificationTokenRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    public String register(RegisterRequest request) {
        var existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isEmpty()) {
            User newUser = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.CUSTOMER)
                    .isVerified(false)
                    .build();
            newUser = userRepository.save(newUser);
            generateAndSendOtp(newUser);
            return "User registered successfully. Please check your email for the OTP.";
        }

        User existingUser = existingUserOpt.get();
        if (existingUser.isVerified()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        // User exists but not verified, update details and resend OTP
        existingUser.setName(request.getName());
        existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        existingUser = userRepository.save(existingUser);
        
        // Delete old tokens for this user
        verificationTokenRepository.deleteByUser(existingUser);
        generateAndSendOtp(existingUser);

        return "User registered successfully. Please check your email for the OTP.";
    }

    private void generateAndSendOtp(User user) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        
        VerificationToken verificationToken = VerificationToken.builder()
                .token(otp)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES))
                .build();
                
        verificationTokenRepository.save(verificationToken);

        String htmlTemplate = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #18181b; padding: 40px; border-radius: 12px; color: #ffffff; text-align: center; border: 1px solid #27272a;\">"
                + "<div style=\"margin-bottom: 30px;\">"
                + "<h1 style=\"color: #ffffff; margin: 0; font-size: 28px; letter-spacing: -0.5px;\">Cine<span style=\"color: #dc2626;\">Reserve</span></h1>"
                + "<p style=\"color: #a1a1aa; font-size: 14px; margin-top: 5px;\">Premium Cinema Club</p>"
                + "</div>"
                + "<div style=\"background-color: #27272a; padding: 30px; border-radius: 8px; margin-bottom: 30px;\">"
                + "<h2 style=\"color: #ffffff; margin-top: 0; font-size: 20px; font-weight: 500;\">Verify your email address</h2>"
                + "<p style=\"color: #d4d4d8; font-size: 15px; line-height: 1.6;\">Thank you for joining Screenly. Please use the verification code below to complete your registration.</p>"
                + "<div style=\"background-color: #18181b; border: 1px dashed #dc2626; padding: 20px; border-radius: 6px; margin: 30px 0;\">"
                + "<span style=\"font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #dc2626;\">" + otp + "</span>"
                + "</div>"
                + "<p style=\"color: #ef4444; font-size: 13px; margin: 0;\">⚠️ This code expires in " + AppConstants.OTP_EXPIRY_MINUTES + " minutes.</p>"
                + "</div>"
                + "<p style=\"color: #71717a; font-size: 12px; margin: 0;\">If you didn't request this email, you can safely ignore it.</p>"
                + "</div>";

        emailService.sendEmail(
                user.getEmail(),
                AppConstants.OTP_EMAIL_SUBJECT,
                htmlTemplate
        );
    }

    public String verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or missing verification token"));

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);
        
        verificationTokenRepository.delete(verificationToken);

        return "Email verified successfully. You can now login.";
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isVerified()) {
            throw new InvalidTokenException("Please verify your email before logging in.");
        }

        var userDetails = new CustomUserDetails(user);
        var jwtToken = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(jwtToken)
                .message("Login successful")
                .build();
    }
}
